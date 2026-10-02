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
}
