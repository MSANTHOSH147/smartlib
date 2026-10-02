package com.smartlib.entity;

import com.smartlib.enums.MemorySource;
import com.smartlib.enums.MemoryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_memories",
    indexes = {
        @Index(name = "idx_user_memory_user", columnList = "user_id"),
        @Index(name = "idx_user_memory_key", columnList = "user_id, memory_key"),
        @Index(name = "idx_user_memory_active", columnList = "user_id, active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 30)
    private MemoryType memoryType;

    @Column(name = "memory_key", nullable = false, length = 100)
    private String key;

    @Column(name = "memory_value", nullable = false, length = 500)
    private String value;

    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MemorySource source = MemorySource.CONVERSATION;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isActiveAndValid() {
        return Boolean.TRUE.equals(active) && !isExpired();
    }
}
