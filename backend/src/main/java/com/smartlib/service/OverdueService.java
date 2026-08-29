package com.smartlib.service;

import com.smartlib.entity.Borrowing;
import com.smartlib.entity.Fine;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.enums.FineStatus;
import com.smartlib.repository.BorrowingRepository;
import com.smartlib.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OverdueService {

    private static final double FINE_PER_DAY = 10.0;

    private final BorrowingRepository borrowingRepository;
    private final FineRepository fineRepository;


    // ============================================================
    // AUTOMATIC OVERDUE + FINE CHECK
    // ============================================================

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOverdueBorrowings() {

        LocalDate today = LocalDate.now();

        List<Borrowing> borrowings =
                borrowingRepository.findByStatusIn(
                        List.of(
                                BorrowStatus.BORROWED,
                                BorrowStatus.OVERDUE
                        )
                );

        for (Borrowing borrowing : borrowings) {

            if (borrowing.getDueDate().isBefore(today)) {

                processOverdueBorrowing(
                        borrowing,
                        today
                );
            }
        }
    }


    // ============================================================
    // PROCESS OVERDUE BORROWING
    // ============================================================

    private void processOverdueBorrowing(
            Borrowing borrowing,
            LocalDate today) {

        // Make sure borrowing is marked OVERDUE
        if (borrowing.getStatus()
                != BorrowStatus.OVERDUE) {

            borrowing.setStatus(
                    BorrowStatus.OVERDUE
            );

            borrowingRepository.save(
                    borrowing
            );
        }


        // --------------------------------------------------------
        // Calculate overdue days
        // --------------------------------------------------------

        long overdueDays =
                ChronoUnit.DAYS.between(
                        borrowing.getDueDate(),
                        today
                );


        double amount =
                overdueDays * FINE_PER_DAY;


        // --------------------------------------------------------
        // Find existing fine
        // --------------------------------------------------------

        Fine fine =
                fineRepository
                        .findByBorrowingId(
                                borrowing.getId()
                        )
                        .orElse(null);


        // --------------------------------------------------------
        // CREATE FINE
        // --------------------------------------------------------

        if (fine == null) {

            Fine newFine =
                    Fine.builder()
                            .borrowing(borrowing)
                            .user(borrowing.getUser())
                            .amount(amount)
                            .status(FineStatus.UNPAID)
                            .createdAt(
                                    LocalDateTime.now()
                            )
                            .build();

            fineRepository.save(newFine);

            return;
        }


        // --------------------------------------------------------
        // UPDATE EXISTING UNPAID FINE
        // --------------------------------------------------------

        if (fine.getStatus()
                == FineStatus.UNPAID) {

            fine.setAmount(amount);

            fineRepository.save(fine);
        }


        // --------------------------------------------------------
        // PAID FINE
        // --------------------------------------------------------
        //
        // Do nothing.
        //
        // A paid fine must NEVER automatically become
        // unpaid again.
        //
    }
}