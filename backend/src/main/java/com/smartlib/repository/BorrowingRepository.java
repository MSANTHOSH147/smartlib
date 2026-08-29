package com.smartlib.repository;

import com.smartlib.entity.Borrowing;
import com.smartlib.enums.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowingRepository
        extends JpaRepository<Borrowing, Long> {

    // ============================================================
    // USER BORROWINGS
    // ============================================================

    List<Borrowing> findByUserId(Long userId);

    List<Borrowing> findByUserIdAndStatus(
            Long userId,
            BorrowStatus status
    );

    // ============================================================
    // BOOK BORROWING CHECK
    // ============================================================

    boolean existsByUserIdAndBookIdAndStatus(
            Long userId,
            Long bookId,
            BorrowStatus status
    );

    // ============================================================
    // PHYSICAL COPY - ACTIVE BORROWING
    // ============================================================

    Optional<Borrowing> findFirstByBookCopyIdAndStatus(
            Long bookCopyId,
            BorrowStatus status
    );

    Optional<Borrowing> findByBookCopyIdAndStatus(
            Long bookCopyId,
            BorrowStatus status
    );



    // ============================================================
    // ADMIN / DASHBOARD
    // ============================================================

    List<Borrowing> findByStatus(
            BorrowStatus status
    );

    List<Borrowing> findByStatusIn(
            List<BorrowStatus> statuses
    );
}
