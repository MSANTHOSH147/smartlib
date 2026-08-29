package com.smartlib.service;

import com.smartlib.dto.fine.FineResponse;
import com.smartlib.entity.Borrowing;
import com.smartlib.entity.Fine;
import com.smartlib.entity.User;
import com.smartlib.enums.FineStatus;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BorrowingRepository;
import com.smartlib.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private static final double FINE_PER_DAY = 10.0;

    private final FineRepository fineRepository;
    private final BorrowingRepository borrowingRepository;


    // ============================================================
    // GET MY FINES
    // ============================================================

    @Transactional(readOnly = true)
    public List<FineResponse> getMyFines(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return fineRepository
                .findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET MY UNPAID FINES
    // ============================================================

    @Transactional(readOnly = true)
    public List<FineResponse> getMyUnpaidFines(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return fineRepository
                .findByUserId(user.getId())
                .stream()
                .filter(fine ->
                        fine.getStatus() == FineStatus.UNPAID
                )
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // CALCULATE FINE
    // ============================================================

    @Transactional
    public FineResponse calculateFine(
            Long borrowingId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Borrowing borrowing =
                borrowingRepository
                        .findById(borrowingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Borrowing record not found"
                                ));


        // --------------------------------------------------------
        // SECURITY CHECK
        // --------------------------------------------------------

        if (!borrowing.getUser().getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    "You cannot calculate a fine for another user's borrowing."
            );
        }


        // --------------------------------------------------------
        // DETERMINE END DATE
        // --------------------------------------------------------

        LocalDate endDate =
                borrowing.getReturnDate() != null
                        ? borrowing.getReturnDate()
                        : LocalDate.now();


        // --------------------------------------------------------
        // CHECK OVERDUE
        // --------------------------------------------------------

        if (!endDate.isAfter(borrowing.getDueDate())) {

            throw new BadRequestException(
                    "This borrowing does not have an overdue fine."
            );
        }


        // --------------------------------------------------------
        // CALCULATE OVERDUE DAYS
        // --------------------------------------------------------

        long overdueDays =
                ChronoUnit.DAYS.between(
                        borrowing.getDueDate(),
                        endDate
                );


        double amount =
                overdueDays * FINE_PER_DAY;


        // --------------------------------------------------------
        // CHECK EXISTING FINE
        // --------------------------------------------------------

        Fine fine =
                fineRepository
                        .findByBorrowingId(borrowingId)
                        .orElse(null);


        if (fine != null) {

            // Already paid
            if (fine.getStatus() == FineStatus.PAID) {

                throw new BadRequestException(
                        "This fine has already been paid."
                );
            }

            // Update existing unpaid fine
            fine.setAmount(amount);

            Fine updated =
                    fineRepository.save(fine);

            return toResponse(updated);
        }


        // --------------------------------------------------------
        // CREATE NEW FINE
        // --------------------------------------------------------

        Fine newFine =
                Fine.builder()
                        .borrowing(borrowing)
                        .user(user)
                        .amount(amount)
                        .status(FineStatus.UNPAID)
                        .createdAt(LocalDateTime.now())
                        .build();


        Fine saved =
                fineRepository.save(newFine);


        return toResponse(saved);
    }


    // ============================================================
    // PAY FINE
    // ============================================================

    @Transactional
    public FineResponse payFine(
            Long fineId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Fine fine =
                fineRepository
                        .findById(fineId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Fine not found"
                                ));


        // --------------------------------------------------------
        // SECURITY CHECK
        // --------------------------------------------------------

        if (!fine.getUser().getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    "You cannot pay another user's fine."
            );
        }


        // --------------------------------------------------------
        // ALREADY PAID
        // --------------------------------------------------------

        if (fine.getStatus() == FineStatus.PAID) {

            throw new BadRequestException(
                    "Fine has already been paid."
            );
        }


        // --------------------------------------------------------
        // MARK AS PAID
        // --------------------------------------------------------

        fine.setStatus(FineStatus.PAID);

        fine.setPaidAt(
                LocalDateTime.now()
        );


        Fine saved =
                fineRepository.save(fine);


        return toResponse(saved);
    }


    // ============================================================
    // CURRENT USER
    // ============================================================

    private User getCurrentUser(
            Authentication authentication) {

        return (User) authentication.getPrincipal();
    }


    // ============================================================
    // ENTITY → DTO
    // ============================================================

    private FineResponse toResponse(
            Fine fine) {

        return FineResponse.builder()

                .id(fine.getId())

                .borrowingId(
                        fine.getBorrowing().getId()
                )

                .userId(
                        fine.getUser().getId()
                )

                .userName(
                        fine.getUser().getName()
                )

                .bookId(
                        fine.getBorrowing()
                                .getBook()
                                .getId()
                )

                .bookTitle(
                        fine.getBorrowing()
                                .getBook()
                                .getTitle()
                )

                .amount(
                        fine.getAmount()
                )

                .status(
                        fine.getStatus()
                )

                .paidAt(
                        fine.getPaidAt()
                )

                .createdAt(
                        fine.getCreatedAt()
                )

                .build();
    }
}