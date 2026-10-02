package com.smartlib.ai.provider;

import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiProviderCapabilities;

public interface AiModelProvider {

    /**
     * Unique identifier for the provider (e.g. "gemini", "groq").
     */
    String getProviderName();

    /**
     * Currently configured model identifier for this provider.
     */
    String getModelName();

    /**
     * Checks if the provider is enabled and configured with required credentials.
     */
    boolean isAvailable();

    /**
     * Generates a chat response supporting text, tool declarations, and tool execution history.
     */
    AiModelResponse generateChat(AiModelRequest request);

    /**
     * Returns capability metadata for this provider.
     */
    default AiProviderCapabilities getCapabilities() {
        return AiProviderCapabilities.builder()
                .supportsToolCalling(true)
                .build();
    }
}
