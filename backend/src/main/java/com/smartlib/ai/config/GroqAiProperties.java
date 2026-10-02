package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class GroqAiProperties {

    @Value("${groq.api-key:${groq.api.key:}}")
    private String apiKey;

    @Value("${groq.base-url:${groq.base.url:https://api.groq.com/openai/v1}}")
    private String baseUrl;

    @Value("${groq.model:${groq.model:llama-3.3-70b-versatile}}")
    private String model;

    @Value("${groq.enabled:${groq.enabled:true}}")
    private boolean enabled;

    @Value("${groq.timeout:${groq.timeout.seconds:30}}")
    private int timeoutSeconds;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.trim().isBlank();
    }
}
