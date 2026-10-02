package com.smartlib.ai.model;

import com.smartlib.ai.dto.gemini.FunctionDeclaration;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AiModelRequest {

    private String systemInstruction;

    @Builder.Default
    private List<AiConversationTurn> turns = new ArrayList<>();

    @Builder.Default
    private List<FunctionDeclaration> tools = Collections.emptyList();

    @Builder.Default
    private boolean useWebGrounding = false;

    /**
     * Checks if this request already contains tool execution results from a previous turn.
     * Used by the router to enforce Step 10: avoid replaying side-effects on a fallback provider mid-tool workflow.
     */
    public boolean hasToolResults() {
        if (turns == null) {
            return false;
        }
        return turns.stream().anyMatch(turn ->
                "tool".equalsIgnoreCase(turn.getRole())
                || (turn.getToolResult() != null && !turn.getToolResult().isEmpty())
        );
    }
}
