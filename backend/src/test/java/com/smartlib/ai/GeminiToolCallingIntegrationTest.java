package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.AiRoutingProperties;
import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.config.GroqAiProperties;
import com.smartlib.ai.dto.AiOrchestrationResult;
import com.smartlib.ai.dto.gemini.*;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiToolCall;
import com.smartlib.ai.provider.GeminiAiModelProvider;
import com.smartlib.ai.provider.GroqAiModelProvider;
import com.smartlib.ai.router.AiModelRouter;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.ai.tools.SmartLibToolExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GeminiToolCallingIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SmartLibToolExecutor toolExecutor;

    @Test
    @DisplayName("Task 14 & 6 & 7: Search books tool loop with real GeminiClient serializing role 'user' for functionResponse")
    void testSearchBooksToolLoop() {
        GeminiAiProperties properties = new GeminiAiProperties();
        properties.setApiKey("test-key-mock");
        properties.setModel("gemini-3.8-flash");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeminiClient geminiClient = new GeminiClient(restClient, properties);
        GeminiAiModelProvider geminiProvider = new GeminiAiModelProvider(geminiClient, properties);

        AiRoutingProperties routingProperties = new AiRoutingProperties();
        routingProperties.setPrimaryProvider("gemini");
        AiModelRouter router = new AiModelRouter(List.of(geminiProvider), routingProperties);

        SmartLibAiOrchestrator orchestrator = new SmartLibAiOrchestrator(router, toolExecutor);

        // Turn 1 response: Gemini returns function call to searchBooks
        String turn1ResponseBody = """
        {
          "candidates": [
            {
              "content": {
                "role": "model",
                "parts": [
                  {
                    "functionCall": {
                      "name": "searchBooks",
                      "args": {
                        "query": "available books"
                      }
                    }
                  }
                ]
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        // Turn 2 response: Gemini receives tool result and produces final answer
        String turn2ResponseBody = """
        {
          "candidates": [
            {
              "content": {
                "role": "model",
                "parts": [
                  {
                    "text": "The library currently has Clean Code and Design Patterns available for borrowing."
                  }
                ]
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        // Expect Turn 1
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key-mock"))
                .andRespond(withSuccess(turn1ResponseBody, MediaType.APPLICATION_JSON));

        // Expect Turn 2: Critical assertion — must NOT contain role:function, MUST contain role:user and functionResponse
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key-mock"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"role\":\"function\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"functionResponse\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"searchBooks\"")))
                .andRespond(withSuccess(turn2ResponseBody, MediaType.APPLICATION_JSON));

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "available books")))
                .thenReturn(Map.of("books", List.of("Clean Code", "Design Patterns")));

        AiOrchestrationResult result = orchestrator.chat("What books are available in the library?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).isEqualTo("The library currently has Clean Code and Design Patterns available for borrowing.");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks");
        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getModel()).isEqualTo("gemini-3.8-flash");

        mockServer.verify();
        verify(toolExecutor, times(1)).executeTool("searchBooks", Map.of("query", "available books"));
    }

    @Test
    @DisplayName("Task 8: Personalized recommendation tool loop executes and formats functionResponse correctly")
    void testPersonalizedRecommendationToolLoop() {
        GeminiAiProperties properties = new GeminiAiProperties();
        properties.setApiKey("test-key-mock");
        properties.setModel("gemini-3.8-flash");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeminiClient geminiClient = new GeminiClient(restClient, properties);
        GeminiAiModelProvider geminiProvider = new GeminiAiModelProvider(geminiClient, properties);

        AiRoutingProperties routingProperties = new AiRoutingProperties();
        routingProperties.setPrimaryProvider("gemini");
        AiModelRouter router = new AiModelRouter(List.of(geminiProvider), routingProperties);

        SmartLibAiOrchestrator orchestrator = new SmartLibAiOrchestrator(router, toolExecutor);

        String turn1Body = """
        {
          "candidates": [
            {
              "content": {
                "role": "model",
                "parts": [
                  {
                    "functionCall": {
                      "name": "getPersonalizedRecommendations",
                      "args": {
                        "limit": 5
                      }
                    }
                  }
                ]
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        String turn2Body = """
        {
          "candidates": [
            {
              "content": {
                "role": "model",
                "parts": [
                  {
                    "text": "Based on your borrowing history, I recommend The Pragmatic Programmer."
                  }
                ]
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(turn1Body, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"role\":\"function\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"getPersonalizedRecommendations\"")))
                .andRespond(withSuccess(turn2Body, MediaType.APPLICATION_JSON));

        when(toolExecutor.executeTool("getPersonalizedRecommendations", Map.of("limit", 5)))
                .thenReturn(Map.of("recommendations", List.of("The Pragmatic Programmer")));

        AiOrchestrationResult result = orchestrator.chat("Recommend books for me based on my borrowing history.");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("The Pragmatic Programmer");
        assertThat(result.getToolCallsExecuted()).containsExactly("getPersonalizedRecommendations");
        mockServer.verify();
    }

    @Test
    @DisplayName("Task 10: Groq serialization still works independently with OpenAI tool_calls format")
    void testGroqSerializationStillWorks() {
        GroqAiProperties groqProps = new GroqAiProperties();
        groqProps.setApiKey("gsk_test");
        groqProps.setModel("llama-3.3-70b-versatile");

        GroqAiModelProvider groqProvider = new GroqAiModelProvider(RestClient.builder().build(), groqProps, objectMapper);

        AiModelRequest request = AiModelRequest.builder()
                .systemInstruction("You are SmartLib AI.")
                .turns(List.of(
                        com.smartlib.ai.model.AiConversationTurn.userTurn("Do you have Clean Code?"),
                        com.smartlib.ai.model.AiConversationTurn.builder()
                                .role("assistant")
                                .toolCalls(List.of(AiToolCall.builder().id("call_abc").name("searchBooks").arguments(Map.of("query", "Clean Code")).build()))
                                .build(),
                        com.smartlib.ai.model.AiConversationTurn.toolTurn("call_abc", "searchBooks", Map.of("found", true))
                ))
                .build();

        Map<String, Object> body = groqProvider.buildRequestBody(request);
        assertThat(body.get("model")).isEqualTo("llama-3.3-70b-versatile");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages).hasSize(4); // system + user + assistant + tool

        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        assertThat(messages.get(2).get("role")).isEqualTo("assistant");
        assertThat(messages.get(3).get("role")).isEqualTo("tool");
        assertThat(messages.get(3).get("tool_call_id")).isEqualTo("call_abc");
    }
}
