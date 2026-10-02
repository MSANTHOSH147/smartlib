package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointStruct {

    private Object id;
    private List<Float> vector;
    private Map<String, Object> payload;

    public static PointStruct of(Object id, List<Float> vector, Map<String, Object> payload) {
        return PointStruct.builder()
                .id(id)
                .vector(vector)
                .payload(payload)
                .build();
    }
}
