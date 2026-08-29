package com.smartlib.repository;

import com.smartlib.entity.Fine;
import com.smartlib.enums.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserId(Long userId);

    List<Fine> findByStatus(FineStatus status);

    Optional<Fine> findByBorrowingId(Long borrowingId);
}