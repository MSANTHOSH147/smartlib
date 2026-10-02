package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QdrantApiResponse<T> {

    private T result;
    private String status;
    private Double time;

    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }
}
