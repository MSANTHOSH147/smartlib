package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiChatResponse {

    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    private UsageMetadata usageMetadata;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
        private GroundingMetadata groundingMetadata;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroundingMetadata {
        private List<String> webSearchQueries;
        private List<GroundingChunk> groundingChunks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroundingChunk {
        private WebSource web;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebSource {
        private String uri;
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptFeedback {
        private String blockReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;
    }

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Candidate candidate = candidates.get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part part : candidate.getContent().getParts()) {
            if (part != null && part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }

    public List<FunctionCall> extractFunctionCalls() {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Candidate candidate = candidates.get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            return Collections.emptyList();
        }
        List<FunctionCall> calls = new ArrayList<>();
        for (Part part : candidate.getContent().getParts()) {
            if (part != null && part.getFunctionCall() != null) {
                calls.add(part.getFunctionCall());
            }
        }
        return calls;
    }

    public boolean hasFunctionCalls() {
        return !extractFunctionCalls().isEmpty();
    }

    public GroundingMetadata extractGroundingMetadata() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0).getGroundingMetadata();
    }
}
