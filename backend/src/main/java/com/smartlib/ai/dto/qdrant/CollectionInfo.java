package com.smartlib.ai.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionInfo {

    private String status;

    @JsonProperty("points_count")
    private Long pointsCount;

    @JsonProperty("vectors_count")
    private Long vectorsCount;

    private CollectionConfig config;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionConfig {
        private CollectionParams params;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionParams {
        private VectorParams vectors;
    }

    public Integer getVectorSize() {
        if (config != null && config.getParams() != null && config.getParams().getVectors() != null) {
            return config.getParams().getVectors().getSize();
        }
        return null;
    }

    public String getVectorDistance() {
        if (config != null && config.getParams() != null && config.getParams().getVectors() != null) {
            return config.getParams().getVectors().getDistance();
        }
        return null;
    }
}
