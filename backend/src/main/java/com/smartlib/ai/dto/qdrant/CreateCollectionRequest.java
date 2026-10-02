package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCollectionRequest {

    private VectorParams vectors;

    public static CreateCollectionRequest of(int size, String distance) {
        return CreateCollectionRequest.builder()
                .vectors(VectorParams.builder()
                        .size(size)
                        .distance(distance)
                        .build())
                .build();
    }
}
