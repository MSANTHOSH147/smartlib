package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpsertPointsRequest {

    private List<PointStruct> points;

    public static UpsertPointsRequest of(List<PointStruct> points) {
        return UpsertPointsRequest.builder()
                .points(points)
                .build();
    }
}
