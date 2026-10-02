package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoredPoint {

    private Object id;
    private Double score;
    private Map<String, Object> payload;
    private Long version;
}
