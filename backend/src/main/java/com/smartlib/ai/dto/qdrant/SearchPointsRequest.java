package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchPointsRequest {

    private List<Float> vector;
    private Integer limit;

    @JsonProperty("with_payload")
    @Builder.Default
    private Boolean withPayload = true;

    private Map<String, Object> filter;

    public static SearchPointsRequest of(List<Float> vector, int limit) {
        return SearchPointsRequest.builder()
                .vector(vector)
                .limit(limit)
                .withPayload(true)
                .build();
    }
}
