package com.smartlib.ai.model;

import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationTurn {

    private String role; // "user", "model", "tool"
    private String content;

    @Builder.Default
    private List<AiToolCall> toolCalls = Collections.emptyList();

    private String toolCallId;
    private String toolName;

    @Builder.Default
    private Map<String, Object> toolResult = Collections.emptyMap();

    public static AiConversationTurn userTurn(String text) {
        return AiConversationTurn.builder()
                .role("user")
                .content(text)
                .build();
    }

    public static AiConversationTurn modelTurn(String text) {
        return AiConversationTurn.builder()
                .role("model")
                .content(text)
                .build();
    }

    public static AiConversationTurn modelTurnWithTools(List<AiToolCall> toolCalls) {
        return AiConversationTurn.builder()
                .role("model")
                .toolCalls(toolCalls != null ? toolCalls : Collections.emptyList())
                .build();
    }

    public static AiConversationTurn toolTurn(String toolCallId, String toolName, Map<String, Object> result) {
        return AiConversationTurn.builder()
                .role("tool")
                .toolCallId(toolCallId)
                .toolName(toolName)
                .toolResult(result != null ? result : Collections.emptyMap())
                .build();
    }
}
