package com.smartlib.ai.dto;

import com.smartlib.ai.model.AiSource;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiOrchestrationResult {

    private String reply;
    @Builder.Default
    private List<String> toolCallsExecuted = new ArrayList<>();
    private boolean success;
    private String errorMessage;
    @Builder.Default
    private List<AiSource> sources = new ArrayList<>();

    public static AiOrchestrationResult success(String reply, List<String> toolsExecuted) {
        return success(reply, toolsExecuted, Collections.emptyList());
    }

    public static AiOrchestrationResult success(String reply, List<String> toolsExecuted, List<AiSource> sources) {
        return AiOrchestrationResult.builder()
                .reply(reply)
                .toolCallsExecuted(toolsExecuted != null ? toolsExecuted : new ArrayList<>())
                .sources(sources != null ? sources : new ArrayList<>())
                .success(true)
                .build();
    }

    public static AiOrchestrationResult error(String errorMessage, String fallbackReply) {
        return AiOrchestrationResult.builder()
                .reply(fallbackReply)
                .success(false)
                .errorMessage(errorMessage)
                .sources(new ArrayList<>())
                .build();
    }
}
