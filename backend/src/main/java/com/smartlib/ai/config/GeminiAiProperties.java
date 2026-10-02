package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class GeminiAiProperties {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.8-flash}")
    private String model = "gemini-3.8-flash";

    @Value("${gemini.embedding.model:gemini-embedding-2}")
    private String embeddingModel = "gemini-embedding-2";

    @Value("${gemini.embedding.dimension:768}")
    private int embeddingDimension = 768;

    @Value("${gemini.enabled:true}")
    private boolean enabled = true;

    @Value("${gemini.timeout.seconds:30}")
    private int timeoutSeconds = 30;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.trim().isBlank();
    }
}
