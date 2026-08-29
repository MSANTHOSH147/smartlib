package com.smartlib.service;

import com.smartlib.dto.admin.AdminDashboardResponse;
import com.smartlib.dto.admin.AdminLibraryResponse;
import com.smartlib.dto.admin.AdminUserResponse;
import com.smartlib.entity.User;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.enums.FineStatus;
import com.smartlib.enums.ReservationStatus;
import com.smartlib.enums.Role;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import com.smartlib.repository.FineRepository;
import com.smartlib.repository.ReservationRepository;
import com.smartlib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartlib.dto.admin.AdminLibraryResponse;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.entity.Borrowing;
import com.smartlib.dto.reservation.ReservationResponse;
import com.smartlib.entity.Reservation;
import com.smartlib.dto.fine.FineResponse;
import com.smartlib.entity.Fine;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRepository borrowingRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;


    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    public AdminDashboardResponse getDashboard() {

        long totalBooks =
                bookRepository.count();

        long totalMembers =
                userRepository.countByRole(
                        Role.MEMBER
                );

        long activeBorrowings =
                borrowingRepository
                        .findByStatus(
                                BorrowStatus.BORROWED
                        )
                        .size();

        long overdueBorrowings =
                borrowingRepository
                        .findByStatus(
                                BorrowStatus.OVERDUE
                        )
                        .size();

        long pendingReservations =
                reservationRepository
                        .findByStatus(
                                ReservationStatus.WAITING
                        )
                        .size();

        long unpaidFines =
                fineRepository
                        .findByStatus(
                                FineStatus.UNPAID
                        )
                        .size();

        return AdminDashboardResponse.builder()
                .totalBooks(totalBooks)
                .totalMembers(totalMembers)
                .activeBorrowings(activeBorrowings)
                .overdueBorrowings(overdueBorrowings)
                .pendingReservations(pendingReservations)
                .unpaidFines(unpaidFines)
                .build();
    }


    // ============================================================
    // GET ALL USERS
    // ============================================================

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }


    // ============================================================
    // GET USER BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(
            Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        return toUserResponse(user);
    }


    // ============================================================
    // ACTIVATE / DEACTIVATE USER
    // ============================================================

    @Transactional
    public AdminUserResponse updateUserStatus(
            Long id,
            boolean active,
            Authentication authentication) {

        User currentAdmin =
                (User) authentication.getPrincipal();

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        // Prevent admin from disabling themselves
        if (user.getId()
                .equals(currentAdmin.getId())) {

            throw new BadRequestException(
                    "You cannot change your own account status."
            );
        }

        user.setActive(active);

        return toUserResponse(
                userRepository.save(user)
        );
    }


    // ============================================================
    // CHANGE USER ROLE
    // ============================================================

    @Transactional
    public AdminUserResponse updateUserRole(
            Long id,
            Role role,
            Authentication authentication) {

        User currentAdmin =
                (User) authentication.getPrincipal();

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        // Prevent changing own role
        if (user.getId()
                .equals(currentAdmin.getId())) {

            throw new BadRequestException(
                    "You cannot change your own role."
            );
        }

        if (role == null) {

            throw new BadRequestException(
                    "Role is required."
            );
        }

        user.setRole(role);

        return toUserResponse(
                userRepository.save(user)
        );
    }


    // ============================================================
    // DELETE USER
    // ============================================================

    @Transactional
    public void deleteUser(
            Long id,
            Authentication authentication) {

        User currentAdmin =
                (User) authentication.getPrincipal();

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        // Prevent self deletion
        if (user.getId()
                .equals(currentAdmin.getId())) {

            throw new BadRequestException(
                    "You cannot delete your own account."
            );
        }

        userRepository.delete(user);
    }


    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private AdminUserResponse toUserResponse(
            User user) {

        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.getActive())
                .build();
    }
    @Transactional(readOnly = true)
public AdminLibraryResponse getLibraryOperations() {

    long totalBorrowings =
            borrowingRepository.count();

    long activeBorrowings =
            borrowingRepository
                    .findByStatus(BorrowStatus.BORROWED)
                    .size();

    long overdueBorrowings =
            borrowingRepository
                    .findByStatus(BorrowStatus.OVERDUE)
                    .size();

    long totalReservations =
            reservationRepository.count();

    long waitingReservations =
            reservationRepository
                    .findByStatus(
                            ReservationStatus.WAITING
                    )
                    .size();

    long readyReservations =
            reservationRepository
                    .findByStatus(
                            ReservationStatus.READY
                    )
                    .size();

    long totalFines =
            fineRepository.count();

    long unpaidFines =
            fineRepository
                    .findByStatus(
                            FineStatus.UNPAID
                    )
                    .size();

    return AdminLibraryResponse.builder()
            .totalBorrowings(totalBorrowings)
            .activeBorrowings(activeBorrowings)
            .overdueBorrowings(overdueBorrowings)
            .totalReservations(totalReservations)
            .waitingReservations(waitingReservations)
            .readyReservations(readyReservations)
            .totalFines(totalFines)
            .unpaidFines(unpaidFines)
            .build();
}
// ============================================================
// GET ALL BORROWINGS
// ============================================================

@Transactional(readOnly = true)
public List<BorrowResponse> getAllBorrowings() {

    return borrowingRepository.findAll()
            .stream()
            .map(this::toBorrowResponse)
            .toList();
}


// ============================================================
// GET ACTIVE BORROWINGS
// ============================================================

@Transactional(readOnly = true)
public List<BorrowResponse> getActiveBorrowings() {

    return borrowingRepository
            .findByStatus(BorrowStatus.BORROWED)
            .stream()
            .map(this::toBorrowResponse)
            .toList();
}


// ============================================================
// GET OVERDUE BORROWINGS
// ============================================================

@Transactional(readOnly = true)
public List<BorrowResponse> getOverdueBorrowings() {

    return borrowingRepository
            .findByStatus(BorrowStatus.OVERDUE)
            .stream()
            .map(this::toBorrowResponse)
            .toList();
}


// ============================================================
// BORROWING RESPONSE MAPPER
// ============================================================

private BorrowResponse toBorrowResponse(
        Borrowing borrowing) {

    return BorrowResponse.builder()
            .id(borrowing.getId())
            .bookId(borrowing.getBook().getId())
            .bookTitle(borrowing.getBook().getTitle())
            .userId(borrowing.getUser().getId())
            .userName(borrowing.getUser().getName())
            .borrowDate(borrowing.getBorrowDate())
            .dueDate(borrowing.getDueDate())
            .returnDate(borrowing.getReturnDate())
            .status(borrowing.getStatus().name())
            .build();
}
// ============================================================
// GET ALL RESERVATIONS
// ============================================================

@Transactional(readOnly = true)
public List<ReservationResponse> getAllReservations() {

    return reservationRepository.findAll()
            .stream()
            .map(this::toReservationResponse)
            .toList();
}


// ============================================================
// GET WAITING RESERVATIONS
// ============================================================

@Transactional(readOnly = true)
public List<ReservationResponse> getWaitingReservations() {

    return reservationRepository
            .findByStatus(ReservationStatus.WAITING)
            .stream()
            .map(this::toReservationResponse)
            .toList();
}


// ============================================================
// GET READY RESERVATIONS
// ============================================================

@Transactional(readOnly = true)
public List<ReservationResponse> getReadyReservations() {

    return reservationRepository
            .findByStatus(ReservationStatus.READY)
            .stream()
            .map(this::toReservationResponse)
            .toList();
}


// ============================================================
// UPDATE RESERVATION STATUS
// ============================================================

@Transactional
public ReservationResponse updateReservationStatus(
        Long reservationId,
        ReservationStatus newStatus) {

    Reservation reservation =
            reservationRepository
                    .findById(reservationId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Reservation not found"
                            ));

    ReservationStatus currentStatus =
            reservation.getStatus();

    // --------------------------------------------------------
    // Validate status
    // --------------------------------------------------------

    if (newStatus == null) {

        throw new BadRequestException(
                "Reservation status is required."
        );
    }

    // --------------------------------------------------------
    // Already completed
    // --------------------------------------------------------

    if (currentStatus == ReservationStatus.COMPLETED) {

        throw new BadRequestException(
                "Completed reservations cannot be changed."
        );
    }

    // --------------------------------------------------------
    // Already cancelled
    // --------------------------------------------------------

    if (currentStatus == ReservationStatus.CANCELLED) {

        throw new BadRequestException(
                "Cancelled reservations cannot be changed."
        );
    }

    // --------------------------------------------------------
    // WAITING → READY
    // WAITING → CANCELLED
    // --------------------------------------------------------

    if (currentStatus == ReservationStatus.WAITING) {

        if (newStatus != ReservationStatus.READY
                && newStatus != ReservationStatus.CANCELLED) {

            throw new BadRequestException(
                    "A waiting reservation can only be marked READY or CANCELLED."
            );
        }
    }

    // --------------------------------------------------------
    // READY → COMPLETED
    // READY → CANCELLED
    // --------------------------------------------------------

    if (currentStatus == ReservationStatus.READY) {

        if (newStatus != ReservationStatus.COMPLETED
                && newStatus != ReservationStatus.CANCELLED) {

            throw new BadRequestException(
                    "A ready reservation can only be marked COMPLETED or CANCELLED."
            );
        }
    }

    reservation.setStatus(newStatus);

    if (newStatus == ReservationStatus.READY) {

        reservation.setReadyAt(
                java.time.LocalDateTime.now()
        );
    }

    Reservation saved =
            reservationRepository.save(reservation);

    return toReservationResponse(saved);
}


// ============================================================
// RESERVATION RESPONSE MAPPER
// ============================================================

private ReservationResponse toReservationResponse(
        Reservation reservation) {

    return ReservationResponse.builder()
            .id(reservation.getId())
            .bookId(reservation.getBook().getId())
            .bookTitle(reservation.getBook().getTitle())
            .userId(reservation.getUser().getId())
            .userName(reservation.getUser().getName())
            .status(reservation.getStatus().name())
            .reservedAt(reservation.getReservedAt())
            .readyAt(reservation.getReadyAt())
            .build();
}
// ============================================================
// GET ALL FINES
// ============================================================

@Transactional(readOnly = true)
public List<FineResponse> getAllFines() {

    return fineRepository.findAll()
            .stream()
            .map(this::toFineResponse)
            .toList();
}


// ============================================================
// GET UNPAID FINES
// ============================================================

@Transactional(readOnly = true)
public List<FineResponse> getUnpaidFines() {

    return fineRepository
            .findByStatus(FineStatus.UNPAID)
            .stream()
            .map(this::toFineResponse)
            .toList();
}


// ============================================================
// PAY FINE AS ADMIN
// ============================================================

@Transactional
public FineResponse payFineAsAdmin(
        Long fineId) {

    Fine fine =
            fineRepository
                    .findById(fineId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Fine not found"
                            ));

    if (fine.getStatus() == FineStatus.PAID) {

        throw new BadRequestException(
                "Fine has already been paid."
        );
    }

    fine.setStatus(FineStatus.PAID);

    fine.setPaidAt(
            java.time.LocalDateTime.now()
    );

    Fine saved =
            fineRepository.save(fine);

    return toFineResponse(saved);
}


// ============================================================
// FINE RESPONSE MAPPER
// ============================================================

private FineResponse toFineResponse(
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
            .amount(fine.getAmount())
            .status(fine.getStatus())
            .paidAt(fine.getPaidAt())
            .createdAt(fine.getCreatedAt())
            .build();
}
}