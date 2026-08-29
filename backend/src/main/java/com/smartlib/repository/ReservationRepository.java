package com.smartlib.repository;

import com.smartlib.entity.Reservation;
import com.smartlib.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByBookId(Long bookId);

    List<Reservation> findByBookIdAndStatus(
            Long bookId,
            ReservationStatus status
    );

    List<Reservation> findByStatus(
            ReservationStatus status
    );

    List<Reservation> findByUserIdAndStatus(
            Long userId,
            ReservationStatus status
    );

    List<Reservation> findByUserIdAndStatusIn(
            Long userId,
            List<ReservationStatus> statuses
    );

    boolean existsByUserIdAndBookIdAndStatusIn(
            Long userId,
            Long bookId,
            List<ReservationStatus> statuses
    );

    /*
     * FIFO reservation lookup.
     *
     * PESSIMISTIC_WRITE prevents two transactions from
     * promoting the same reservation simultaneously.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.book.id = :bookId
        AND r.status = :status
        ORDER BY r.reservedAt ASC, r.id ASC
    """)
    List<Reservation> findWaitingReservationsForUpdate(
            @Param("bookId") Long bookId,
            @Param("status") ReservationStatus status
    );
    Reservation findFirstByBookIdAndStatusOrderByReservedAtAsc(
        Long bookId,
        ReservationStatus status
);

    /*
     * Convenience method used by return processing.
     */
    default Optional<Reservation> findOldestWaitingForUpdate(
            Long bookId) {

        return findWaitingReservationsForUpdate(
                bookId,
                ReservationStatus.WAITING
        )
        .stream()
        .findFirst();
    }
}