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

        if (request != null && request.getContents() != null) {
            for (com.smartlib.ai.dto.gemini.Content c : request.getContents()) {
                if (c != null) {
                    if ("function".equalsIgnoreCase(c.getRole()) || "tool".equalsIgnoreCase(c.getRole())) {
                        c.setRole("user");
                    } else if ("assistant".equalsIgnoreCase(c.getRole())) {
                        c.setRole("model");
                    } else if (!"model".equalsIgnoreCase(c.getRole())) {
                        c.setRole("user");
                    }
                }
            }
        }

        String model = properties.getModel();
        try {
            return geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .build(model))
                    .header("x-goog-api-key", properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiChatResponse.class);
        } catch (RestClientResponseException ex) {
            String sanitizedBody = sanitizeErrorBody(ex.getResponseBodyAsString());
            log.error("Gemini chat API error: status={}, errorSummary={}", ex.getStatusCode(), sanitizedBody);
            throw new RuntimeException("Gemini API request failed with status: " + ex.getStatusCode().value() + " - " + sanitizedBody, ex);
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
                            .build(embeddingModel))
                    .header("x-goog-api-key", properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiEmbeddingResponse.class);

            if (response != null && response.getEmbedding() != null) {
                return response.getValues();
            }
            return Collections.emptyList();
        } catch (RestClientResponseException ex) {
            log.error("Gemini embedding API error: status={}, errorSummary={}", ex.getStatusCode(), sanitizeErrorBody(ex.getResponseBodyAsString()));
            throw new RuntimeException("Gemini embedding API failed with status: " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Gemini embedding communication error: {}", ex.getMessage());
            throw new RuntimeException("Failed to generate embedding with Gemini API: " + ex.getMessage(), ex);
        }
    }

    private String sanitizeErrorBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "empty_body";
        }
        String sanitized = rawBody.replaceAll("AIza[0-9A-Za-z-_]{30,}", "***")
                .replaceAll("Bearer\\s+[A-Za-z0-9-_.]+", "***");
        return sanitized.length() > 300 ? sanitized.substring(0, 300) + "..." : sanitized;
    }
}
