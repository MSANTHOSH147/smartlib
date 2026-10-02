package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.GroqAiProperties;
import com.smartlib.ai.dto.gemini.FunctionDeclaration;
import com.smartlib.ai.model.AiConversationTurn;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiToolCall;
import com.smartlib.ai.provider.GroqAiModelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GroqAiModelProviderTest {

    @Mock
    private RestClient groqRestClient;

    private GroqAiProperties properties;
    private ObjectMapper objectMapper;
    private GroqAiModelProvider provider;

    @BeforeEach
    void setUp() {
        properties = new GroqAiProperties();
        properties.setApiKey("test-dummy-api-key");
        properties.setModel("llama-3.3-70b-versatile");
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.groq.com/openai/v1");

        objectMapper = new ObjectMapper();
        provider = new GroqAiModelProvider(groqRestClient, properties, objectMapper);
    }

    @Test
    @DisplayName("16. GroqAiModelProvider constructs OpenAI-compatible chat request")
    void testRequestConstruction() {
        AiModelRequest request = AiModelRequest.builder()
                .systemInstruction("You are SmartLib Assistant.")
                .turns(List.of(
                        AiConversationTurn.userTurn("Hi"),
                        AiConversationTurn.modelTurn("Hello! How can I help you today?"),
                        AiConversationTurn.userTurn("Find me a book on AI")
                ))
                .build();

        Map<String, Object> body = provider.buildRequestBody(request);

        assertThat(body.get("model")).isEqualTo("llama-3.3-70b-versatile");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");

        assertThat(messages).hasSize(4);
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("You are SmartLib Assistant.");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        assertThat(messages.get(1).get("content")).isEqualTo("Hi");
        assertThat(messages.get(2).get("role")).isEqualTo("assistant");
        assertThat(messages.get(3).get("role")).isEqualTo("user");
        assertThat(messages.get(3).get("content")).isEqualTo("Find me a book on AI");
    }

    @Test
    @DisplayName("17. Tool schema conversion normalizes UPPERCASE types to standard lowercase JSON schema")
    void testToolSchemaConversion() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "query", Map.of("type", "STRING", "description", "Search query"),
                        "limit", Map.of("type", "INTEGER", "description", "Max count")
                ),
                "required", List.of("query")
        );

        FunctionDeclaration decl = FunctionDeclaration.builder()
                .name("searchBooks")
                .description("Search book catalog")
                .parameters(params)
                .build();

        Map<String, Object> tool = provider.convertToolSchema(decl);

        assertThat(tool.get("type")).isEqualTo("function");
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) tool.get("function");
        assertThat(function.get("name")).isEqualTo("searchBooks");
        assertThat(function.get("description")).isEqualTo("Search book catalog");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertThat(parameters.get("type")).isEqualTo("object");

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) parameters.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> queryProp = (Map<String, Object>) props.get("query");
        assertThat(queryProp.get("type")).isEqualTo("string");
    }

    @Test
    @DisplayName("18. Parse Groq response containing tool_calls into AiToolCall list")
    void testToolCallResponseParsing() {
        String groqJson = """
                {
                  "id": "chatcmpl-12345",
                  "model": "llama-3.3-70b-versatile",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "call_abc_99",
                            "type": "function",
                            "function": {
                              "name": "searchBooks",
                              "arguments": "{\\"query\\":\\"Spring Boot\\"}"
                            }
                          }
                        ]
                      },
                      "finish_reason": "tool_calls"
                    }
                  ]
                }
                """;

        AiModelResponse response = provider.parseResponse(groqJson);

        assertThat(response.getProvider()).isEqualTo("groq");
        assertThat(response.getModel()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(response.hasToolCalls()).isTrue();
        assertThat(response.getToolCalls()).hasSize(1);

        AiToolCall call = response.getToolCalls().get(0);
        assertThat(call.getId()).isEqualTo("call_abc_99");
        assertThat(call.getName()).isEqualTo("searchBooks");
        assertThat(call.getArguments()).containsEntry("query", "Spring Boot");
    }

    @Test
    @DisplayName("19. Parse Groq text response into normalized AiModelResponse")
    void testTextResponseParsing() {
        String groqJson = """
                {
                  "id": "chatcmpl-67890",
                  "model": "llama-3.3-70b-versatile",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "The library is open from 9 AM to 8 PM."
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        AiModelResponse response = provider.parseResponse(groqJson);

        assertThat(response.getProvider()).isEqualTo("groq");
        assertThat(response.hasText()).isTrue();
        assertThat(response.getText()).isEqualTo("The library is open from 9 AM to 8 PM.");
        assertThat(response.hasToolCalls()).isFalse();
        assertThat(response.getFinishReason()).isEqualTo("stop");
    }

    @Test
    @DisplayName("20. Provider error handling when unconfigured or disabled")
    void testProviderErrorHandling() {
        properties.setEnabled(false);

        assertThat(provider.isAvailable()).isFalse();

        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(AiConversationTurn.userTurn("Hello")))
                .build();

        assertThatThrownBy(() -> provider.generateChat(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Groq AI service is not configured or disabled");
    }
}
