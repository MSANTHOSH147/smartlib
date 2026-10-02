package com.smartlib.ai.model;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AiModelResponse {

    private String text;

    @Builder.Default
    private List<AiToolCall> toolCalls = Collections.emptyList();

    private String provider;
    private String model;
    private String finishReason;
    private String error;

    @Builder.Default
    private List<AiSource> sources = Collections.emptyList();

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
