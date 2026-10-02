package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiEmbeddingResponse {

    private ContentEmbedding embedding;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentEmbedding {
        private List<Float> values;
    }

    public List<Float> getValues() {
        if (embedding != null && embedding.getValues() != null) {
            return embedding.getValues();
        }
        return Collections.emptyList();
    }
}
