package com.smartlib.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.gemini.Content;
import com.smartlib.ai.dto.gemini.FunctionCall;
import com.smartlib.ai.dto.gemini.GeminiChatRequest;
import com.smartlib.ai.dto.gemini.GeminiChatResponse;
import com.smartlib.ai.dto.gemini.Part;
import com.smartlib.ai.model.AiConversationTurn;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiToolCall;
import com.smartlib.ai.provider.GeminiAiModelProvider;
import com.smartlib.ai.service.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiThoughtSignatureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GeminiAiProperties properties;

    private GeminiAiModelProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getModel()).thenReturn("gemini-3.8-flash");
        lenient().when(geminiClient.isAvailable()).thenReturn(true);
        provider = new GeminiAiModelProvider(geminiClient, properties);
    }

    @Test
    @DisplayName("Gemini JSON with a functionCall and sibling thoughtSignature deserializes correctly and preserves signature")
    void testGeminiResponseDeserializationPreservesThoughtSignature() throws Exception {
        String json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "functionCall": {
                      "name": "searchBooks",
                      "args": {
                        "query": "Effective Java"
                      }
                    },
                    "thoughtSignature": "sig_abc123_gemini3_thought"
                  }
                ],
                "role": "model"
              },
              "finishReason": "STOP",
              "index": 0
            }
          ]
        }
        """;

        GeminiChatResponse response = objectMapper.readValue(json, GeminiChatResponse.class);

        assertNotNull(response);
        assertNotNull(response.getCandidates());
        assertEquals(1, response.getCandidates().size());

        Part part = response.getCandidates().get(0).getContent().getParts().get(0);
        assertNotNull(part);
        assertEquals("sig_abc123_gemini3_thought", part.getThoughtSignature());
        assertNotNull(part.getFunctionCall());
        assertEquals("searchBooks", part.getFunctionCall().getName());

        // Verify extractFunctionCalls() preserves thoughtSignature
        List<FunctionCall> functionCalls = response.extractFunctionCalls();
        assertEquals(1, functionCalls.size());
        FunctionCall fc = functionCalls.get(0);
        assertEquals("searchBooks", fc.getName());
        assertEquals("Effective Java", fc.getArgs().get("query"));
        assertEquals("sig_abc123_gemini3_thought", fc.getThoughtSignature());
    }

    @Test
    @DisplayName("Part serialization puts thoughtSignature beside functionCall, NOT inside functionCall")
    void testPartSerializationPlacesThoughtSignatureAsSibling() throws Exception {
        Part part = Part.fromFunctionCall("searchBooks", Map.of("query", "Spring Boot"), "sig_sibling_test_789");

        String json = objectMapper.writeValueAsString(part);
        JsonNode root = objectMapper.readTree(json);

        // Root Part must contain thoughtSignature as a direct property
        assertTrue(root.has("thoughtSignature"), "Part JSON must have thoughtSignature at root level");
        assertEquals("sig_sibling_test_789", root.get("thoughtSignature").asText());

        // Root Part must contain functionCall
        assertTrue(root.has("functionCall"), "Part JSON must have functionCall at root level");
        JsonNode functionCallNode = root.get("functionCall");
        assertEquals("searchBooks", functionCallNode.get("name").asText());
        assertEquals("Spring Boot", functionCallNode.get("args").get("query").asText());

        // thoughtSignature MUST NOT be serialized inside the nested functionCall object
        assertFalse(functionCallNode.has("thoughtSignature"),
                "thoughtSignature MUST NOT be inside nested functionCall JSON object");
    }

    @Test
    @DisplayName("GeminiAiModelProvider.toGeminiChatRequest() preserves thoughtSignature when reconstructing model turn")
    void testToGeminiChatRequestPreservesThoughtSignature() {
        AiConversationTurn userTurn = AiConversationTurn.userTurn("Find Clean Architecture");
        AiConversationTurn modelTurn = AiConversationTurn.builder()
                .role("model")
                .toolCalls(List.of(
                        AiToolCall.builder()
                                .name("searchBooks")
                                .arguments(Map.of("query", "Clean Architecture"))
                                .thoughtSignature("sig_recon_turn_456")
                                .build()
                ))
                .build();
        AiConversationTurn toolTurn = AiConversationTurn.toolTurn("call_123", "searchBooks", Map.of("found", 1));

        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(userTurn, modelTurn, toolTurn))
                .build();

        GeminiChatRequest geminiRequest = provider.toGeminiChatRequest(request);

        assertNotNull(geminiRequest);
        List<Content> contents = geminiRequest.getContents();
        assertEquals(3, contents.size());

        // Turn 1: user
        assertEquals("user", contents.get(0).getRole());

        // Turn 2: model turn with reconstructed function call and thoughtSignature
        Content modelContent = contents.get(1);
        assertEquals("model", modelContent.getRole());
        assertEquals(1, modelContent.getParts().size());
        Part reconstructedPart = modelContent.getParts().get(0);
        assertNotNull(reconstructedPart.getFunctionCall());
        assertEquals("searchBooks", reconstructedPart.getFunctionCall().getName());
        assertEquals("sig_recon_turn_456", reconstructedPart.getThoughtSignature());

        // Turn 3: function response turn MUST have role="user"
        Content toolContent = contents.get(2);
        assertEquals("user", toolContent.getRole(), "Function response Content must have role='user' for Gemini API");
        assertNotNull(toolContent.getParts().get(0).getFunctionResponse());
        assertEquals("searchBooks", toolContent.getParts().get(0).getFunctionResponse().getName());
    }

    @Test
    @DisplayName("GeminiAiModelProvider.generateChat() preserves thoughtSignature in AiToolCall")
    void testGenerateChatPreservesThoughtSignature() {
        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.builder()
                                        .role("model")
                                        .parts(List.of(
                                                Part.fromFunctionCall("getRecommendations", Map.of("count", 5), "sig_rec_999")
                                        ))
                                        .build())
                                .finishReason("STOP")
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any(GeminiChatRequest.class))).thenReturn(mockResponse);

        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(AiConversationTurn.userTurn("Recommend me some books")))
                .build();

        AiModelResponse response = provider.generateChat(request);

        assertNotNull(response);
        assertEquals(1, response.getToolCalls().size());
        AiToolCall tc = response.getToolCalls().get(0);
        assertEquals("getRecommendations", tc.getName());
        assertEquals("sig_rec_999", tc.getThoughtSignature());
    }

    @Test
    @DisplayName("Null thoughtSignature is valid and omitted from Part JSON serialization")
    void testNullThoughtSignatureOmittedFromSerialization() throws Exception {
        Part part = Part.fromFunctionCall("searchBooks", Map.of("query", "Java"));
        assertNull(part.getThoughtSignature());

        String json = objectMapper.writeValueAsString(part);
        JsonNode root = objectMapper.readTree(json);

        assertFalse(root.has("thoughtSignature"), "Null thoughtSignature must be omitted due to NON_NULL include");
        assertTrue(root.has("functionCall"));
    }
}
