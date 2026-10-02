package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.gemini.*;
import com.smartlib.ai.service.GeminiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class GeminiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GeminiClient detects missing API key and does not crash")
    void testMissingApiKeyBehavior() {
        GeminiAiProperties properties = new GeminiAiProperties();
        properties.setApiKey("");
        properties.setModel("gemini-3.8-flash");
        properties.setEmbeddingModel("gemini-embedding-2");
        properties.setEmbeddingDimension(768);

        RestClient restClient = RestClient.builder().build();
        GeminiClient client = new GeminiClient(restClient, properties);

        assertFalse(client.isAvailable());

        assertThrows(IllegalStateException.class, () ->
                client.generateChat(GeminiChatRequest.builder().build())
        );

        assertThrows(IllegalStateException.class, () ->
                client.generateEmbedding("Test query")
        );
    }

    @Test
    @DisplayName("GeminiClient successfully calls generateContent with mock server")
    void testGenerateChatWithMockServer() throws Exception {
        GeminiAiProperties properties = new GeminiAiProperties();
        properties.setApiKey("test-fake-key-12345");
        properties.setModel("gemini-3.8-flash");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeminiClient client = new GeminiClient(restClient, properties);
        assertTrue(client.isAvailable());

        String mockResponseBody = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  { "text": "Clean Code is a handbook of agile software craftsmanship." }
                ],
                "role": "model"
              },
              "finishReason": "STOP"
            }
          ]
        }
        """;

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=test-fake-key-12345"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        GeminiChatRequest request = GeminiChatRequest.builder()
                .contents(List.of(Content.user("What is Clean Code?")))
                .build();

        GeminiChatResponse response = client.generateChat(request);

        assertNotNull(response);
        assertEquals("Clean Code is a handbook of agile software craftsmanship.", response.extractText());
        server.verify();
    }

    @Test
    @DisplayName("GeminiClient successfully calls embedContent with 768 dimensions")
    void testGenerateEmbeddingWithMockServer() {
        GeminiAiProperties properties = new GeminiAiProperties();
        properties.setApiKey("test-fake-key-12345");
        properties.setEmbeddingModel("gemini-embedding-2");
        properties.setEmbeddingDimension(768);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeminiClient client = new GeminiClient(restClient, properties);

        String mockResponseBody = """
        {
          "embedding": {
            "values": [0.12, 0.34, -0.56]
          }
        }
        """;

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:embedContent?key=test-fake-key-12345"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        List<Float> embedding = client.generateEmbedding("Clean Code");

        assertNotNull(embedding);
        assertEquals(3, embedding.size());
        assertEquals(0.12f, embedding.get(0), 0.001f);
        assertEquals(0.34f, embedding.get(1), 0.001f);
        assertEquals(-0.56f, embedding.get(2), 0.001f);
        server.verify();
    }
}
