package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.dto.gemini.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GeminiChatRequest serializes to expected Gemini REST format")
    void testChatRequestSerialization() throws Exception {
        GeminiChatRequest request = GeminiChatRequest.builder()
                .contents(List.of(Content.user("Hello, who wrote Clean Code?")))
                .generationConfig(GeminiChatRequest.GenerationConfig.builder()
                        .temperature(0.7)
                        .maxOutputTokens(1000)
                        .build())
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"contents\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"text\":\"Hello, who wrote Clean Code?\""));
        assertTrue(json.contains("\"temperature\":0.7"));
    }

    @Test
    @DisplayName("GeminiChatResponse deserializes and extracts text properly")
    void testChatResponseDeserialization() throws Exception {
        String json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  { "text": "Clean Code was written by Robert C. Martin." }
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
        assertEquals("Clean Code was written by Robert C. Martin.", response.extractText());
    }

    @Test
    @DisplayName("GeminiEmbeddingRequest serializes with content and outputDimensionality 768")
    void testEmbeddingRequestSerialization() throws Exception {
        GeminiEmbeddingRequest request = GeminiEmbeddingRequest.of("Clean Code by Robert C. Martin", 768);

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"outputDimensionality\":768"));
        assertTrue(json.contains("\"parts\""));
        assertTrue(json.contains("\"text\":\"Clean Code by Robert C. Martin\""));
        assertFalse(json.contains("task_type"), "Must not contain obsolete task_type fields");
    }

    @Test
    @DisplayName("GeminiEmbeddingResponse deserializes embedding values accurately")
    void testEmbeddingResponseDeserialization() throws Exception {
        String json = """
        {
          "embedding": {
            "values": [0.0123, -0.0456, 0.0789]
          }
        }
        """;

        GeminiEmbeddingResponse response = objectMapper.readValue(json, GeminiEmbeddingResponse.class);

        assertNotNull(response);
        assertNotNull(response.getEmbedding());
        List<Float> values = response.getValues();
        assertEquals(3, values.size());
        assertEquals(0.0123f, values.get(0), 0.0001f);
        assertEquals(-0.0456f, values.get(1), 0.0001f);
        assertEquals(0.0789f, values.get(2), 0.0001f);
    }

    @Test
    @DisplayName("1. testGeminiUserRoleSerialization: User turn serializes with role 'user'")
    void testGeminiUserRoleSerialization() throws Exception {
        Content userContent = Content.user("Hello library");
        String json = objectMapper.writeValueAsString(userContent);

        assertTrue(json.contains("\"role\":\"user\""));
        assertFalse(json.contains("\"role\":\"function\""));
        assertFalse(json.contains("\"role\":\"assistant\""));
    }

    @Test
    @DisplayName("2. testGeminiModelRoleSerialization: Model turn serializes with role 'model'")
    void testGeminiModelRoleSerialization() throws Exception {
        Content modelContent = Content.model("Welcome to SmartLib!");
        String json = objectMapper.writeValueAsString(modelContent);

        assertTrue(json.contains("\"role\":\"model\""));
        assertFalse(json.contains("\"role\":\"function\""));
        assertFalse(json.contains("\"role\":\"assistant\""));
    }

    @Test
    @DisplayName("3. testGeminiFunctionResponseUsesUserRole: Function response uses Gemini-supported role 'user'")
    void testGeminiFunctionResponseUsesUserRole() throws Exception {
        Content functionResponseContent = Content.functionResponse("searchBooks", java.util.Map.of("books", List.of("Clean Code")));
        String json = objectMapper.writeValueAsString(functionResponseContent);

        assertTrue(json.contains("\"role\":\"user\""), "Gemini requires role 'user' for functionResponse");
        assertTrue(json.contains("\"functionResponse\""));
        assertTrue(json.contains("\"name\":\"searchBooks\""));
    }

    @Test
    @DisplayName("4. testGeminiFunctionResponseDoesNotUseFunctionRole: Function response NEVER serializes with role 'function'")
    void testGeminiFunctionResponseDoesNotUseFunctionRole() throws Exception {
        Content functionResponseContent = Content.functionResponse("checkBookAvailability", java.util.Map.of("available", true));
        String json = objectMapper.writeValueAsString(functionResponseContent);

        assertFalse(json.contains("\"role\":\"function\""), "Gemini API strictly rejects role 'function'");
        assertFalse(json.contains("\"role\":\"assistant\""));
    }

    @Test
    @DisplayName("5. testGeminiFunctionCallParsing: Deserializes functionCall from candidate correctly")
    void testGeminiFunctionCallParsing() throws Exception {
        String json = """
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

        GeminiChatResponse response = objectMapper.readValue(json, GeminiChatResponse.class);
        assertTrue(response.hasFunctionCalls());
        List<FunctionCall> calls = response.extractFunctionCalls();
        assertEquals(1, calls.size());
        assertEquals("searchBooks", calls.get(0).getName());
        assertEquals("available books", calls.get(0).getArgs().get("query"));
    }

    @Test
    @DisplayName("9. testGeminiRequestContainsNoInvalidFunctionRole: Multi-turn request contains strictly 'user' or 'model' roles")
    void testGeminiRequestContainsNoInvalidFunctionRole() throws Exception {
        Part userPart = Part.fromText("What books are available?");
        Part funcCallPart = Part.fromFunctionCall("searchBooks", java.util.Map.of("query", "available"));
        Part funcRespPart = Part.fromFunctionResponse("searchBooks", java.util.Map.of("results", List.of("Clean Code")));

        GeminiChatRequest request = GeminiChatRequest.builder()
                .contents(List.of(
                        Content.builder().role("user").parts(List.of(userPart)).build(),
                        Content.builder().role("model").parts(List.of(funcCallPart)).build(),
                        Content.functionResponses(List.of(funcRespPart))
                ))
                .build();

        String json = objectMapper.writeValueAsString(request);
        assertFalse(json.contains("\"role\":\"function\""), "Gemini payload must never contain role:function");
        assertFalse(json.contains("\"role\":\"assistant\""), "Gemini payload must never contain role:assistant");
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"role\":\"model\""));
    }

    @Test
    @DisplayName("11. testConfiguredGeminiModelIsUsed: GeminiAiProperties reads model from configuration")
    void testConfiguredGeminiModelIsUsed() {
        com.smartlib.ai.config.GeminiAiProperties properties = new com.smartlib.ai.config.GeminiAiProperties();
        properties.setModel("gemini-3.8-flash");
        assertEquals("gemini-3.8-flash", properties.getModel());

        properties.setModel("custom-gemini-override");
        assertEquals("custom-gemini-override", properties.getModel());
    }

    @Test
    @DisplayName("12. testDefaultGeminiModelIsCorrect: Default Gemini model is gemini-3.8-flash")
    void testDefaultGeminiModelIsCorrect() {
        com.smartlib.ai.config.GeminiAiProperties properties = new com.smartlib.ai.config.GeminiAiProperties();
        assertEquals("gemini-3.8-flash", properties.getModel(), "Default Gemini model must be gemini-3.8-flash");
        assertEquals("gemini-embedding-2", properties.getEmbeddingModel());
        assertEquals(768, properties.getEmbeddingDimension());
    }
}
