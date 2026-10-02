package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AiRoutingProperties {

    @Value("${ai.primary-provider:${ai.primary.provider:gemini}}")
    private String primaryProvider;

    @Value("${ai.fallback-provider:${ai.fallback.provider:groq}}")
    private String fallbackProvider;

    @Value("${ai.routing.cooldown-seconds:60}")
    private int cooldownSeconds;
}
