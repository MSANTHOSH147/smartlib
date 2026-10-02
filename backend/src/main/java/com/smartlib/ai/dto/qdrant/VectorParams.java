package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorParams {

    private Integer size;
    private String distance;

    public static VectorParams cosine(int size) {
        return VectorParams.builder()
                .size(size)
                .distance("Cosine")
                .build();
    }
}
