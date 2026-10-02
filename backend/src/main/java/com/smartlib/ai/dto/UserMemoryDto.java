package com.smartlib.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartlib.enums.MemorySource;
import com.smartlib.enums.MemoryType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserMemoryDto {
    private Long id;
    private MemoryType memoryType;
    private String key;
    private String value;
    private Double confidence;
    private MemorySource source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private Boolean active;

    public static UserMemoryDto fromEntity(com.smartlib.entity.UserMemory entity) {
        if (entity == null) return null;
        return UserMemoryDto.builder()
                .id(entity.getId())
                .memoryType(entity.getMemoryType())
                .key(entity.getKey())
                .value(entity.getValue())
                .confidence(entity.getConfidence())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .expiresAt(entity.getExpiresAt())
                .active(entity.getActive())
                .build();
    }
}
