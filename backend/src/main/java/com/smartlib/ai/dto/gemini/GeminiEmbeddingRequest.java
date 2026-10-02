package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiEmbeddingRequest {

    private Content content;
    private Integer outputDimensionality;

    public static GeminiEmbeddingRequest of(String text, int outputDimensionality) {
        return GeminiEmbeddingRequest.builder()
                .content(Content.builder()
                        .parts(List.of(Part.fromText(text)))
                        .build())
                .outputDimensionality(outputDimensionality)
                .build();
    }
}
