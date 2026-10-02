package com.smartlib.ai.service;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.gemini.GeminiChatRequest;
import com.smartlib.ai.dto.gemini.GeminiChatResponse;
import com.smartlib.ai.dto.gemini.GeminiEmbeddingRequest;
import com.smartlib.ai.dto.gemini.GeminiEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final RestClient geminiRestClient;
    private final GeminiAiProperties properties;

    public boolean isAvailable() {
        return properties.isConfigured();
    }

    public GeminiChatResponse generateChat(GeminiChatRequest request) {
        if (!isAvailable()) {
            log.warn("Gemini chat requested but GEMINI_API_KEY is not configured.");
            throw new IllegalStateException("Gemini AI service is not configured. GEMINI_API_KEY is missing.");
        }

        String model = properties.getModel();
        try {
            return geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.getApiKey().trim())
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiChatResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("Gemini chat API error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Gemini API request failed with status: " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Gemini chat communication error: {}", ex.getMessage());
            throw new RuntimeException("Failed to communicate with Gemini API: " + ex.getMessage(), ex);
        }
    }

    public List<Float> generateEmbedding(String text) {
        if (!isAvailable()) {
            log.warn("Gemini embedding requested but GEMINI_API_KEY is not configured.");
            throw new IllegalStateException("Gemini AI service is not configured. GEMINI_API_KEY is missing.");
        }

        if (text == null || text.trim().isBlank()) {
            return Collections.emptyList();
        }

        String embeddingModel = properties.getEmbeddingModel();
        int dimension = properties.getEmbeddingDimension();
        GeminiEmbeddingRequest request = GeminiEmbeddingRequest.of(text, dimension);

        try {
            GeminiEmbeddingResponse response = geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:embedContent")
                            .queryParam("key", properties.getApiKey().trim())
                            .build(embeddingModel))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiEmbeddingResponse.class);

            if (response != null && response.getEmbedding() != null) {
                return response.getValues();
            }
            return Collections.emptyList();
        } catch (RestClientResponseException ex) {
            log.error("Gemini embedding API error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Gemini embedding API failed with status: " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Gemini embedding communication error: {}", ex.getMessage());
            throw new RuntimeException("Failed to generate embedding with Gemini API: " + ex.getMessage(), ex);
        }
    }
}
