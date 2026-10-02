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
public class DeletePointsRequest {

    private List<Object> points;

    public static DeletePointsRequest of(Object... pointIds) {
        return DeletePointsRequest.builder()
                .points(List.of(pointIds))
                .build();
    }
}
