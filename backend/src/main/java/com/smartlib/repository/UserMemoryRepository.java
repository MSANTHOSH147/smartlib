package com.smartlib.repository;

import com.smartlib.entity.User;
import com.smartlib.entity.UserMemory;
import com.smartlib.enums.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {

    List<UserMemory> findByUserAndActiveTrue(User user);

    Optional<UserMemory> findByUserAndKeyAndActiveTrue(User user, String key);

    List<UserMemory> findByUserAndMemoryTypeAndActiveTrue(User user, MemoryType memoryType);

    Optional<UserMemory> findByIdAndUser(Long id, User user);

    @Query("SELECT m FROM UserMemory m WHERE m.user = :user AND m.active = true AND (m.expiresAt IS NULL OR m.expiresAt > :now) ORDER BY m.updatedAt DESC, m.createdAt DESC")
    List<UserMemory> findActiveByUserAndNotExpired(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT m FROM UserMemory m WHERE m.user.id = :userId AND m.active = true AND (m.expiresAt IS NULL OR m.expiresAt > :now) ORDER BY m.confidence DESC, m.createdAt DESC")
    List<UserMemory> findActiveByUserIdAndNotExpired(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
