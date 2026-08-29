package com.smartlib.repository;

import com.smartlib.entity.BookCopy;
import com.smartlib.enums.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository
        extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookId(Long bookId);

    List<BookCopy> findByBookIdAndStatus(
            Long bookId,
            BookCopyStatus status
    );

    long countByBookId(Long bookId);

    long countByBookIdAndStatus(
            Long bookId,
            BookCopyStatus status
    );

    boolean existsByBookIdAndCopyNumber(
            Long bookId,
            String copyNumber
    );

    Optional<BookCopy> findByQrToken(String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM BookCopy c
        WHERE c.book.id = :bookId
        AND c.status = :status
        ORDER BY c.id ASC
    """)
    List<BookCopy> findAvailableCopiesForUpdate(
            @Param("bookId") Long bookId,
            @Param("status") BookCopyStatus status
    );
}