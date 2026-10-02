package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.memory")
@Getter
@Setter
public class AiMemoryProperties {

    /**
     * Whether user memory extraction and context injection is enabled.
     */
    private boolean enabled = true;

    /**
     * Minimum confidence threshold required to persist a candidate memory.
     */
    private double minConfidence = 0.75;

    /**
     * Maximum number of user memories injected into a chat turn context.
     */
    private int maxContextMemories = 10;
}
