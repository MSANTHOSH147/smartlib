package com.smartlib.controller;

import com.smartlib.dto.admin.AdminDashboardResponse;
import com.smartlib.dto.admin.AdminLibraryResponse;
import com.smartlib.dto.admin.AdminUserResponse;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.dto.fine.FineResponse;
import com.smartlib.dto.reservation.ReservationResponse;
import com.smartlib.enums.ReservationStatus;
import com.smartlib.enums.Role;
import com.smartlib.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    // ============================================================
    // DASHBOARD
    // ============================================================

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse>
    getDashboard() {

        return ResponseEntity.ok(
                adminService.getDashboard()
        );
    }


    // ============================================================
    // USERS
    // ============================================================

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>>
    getAllUsers() {

        return ResponseEntity.ok(
                adminService.getAllUsers()
        );
    }


    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse>
    getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminService.getUserById(id)
        );
    }


    @PutMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse>
    updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminService.updateUserStatus(
                        id,
                        active,
                        authentication
                )
        );
    }


    @PutMapping("/users/{id}/role")
    public ResponseEntity<AdminUserResponse>
    updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role,
            Authentication authentication) {

        return ResponseEntity.ok(
                adminService.updateUserRole(
                        id,
                        role,
                        authentication
                )
        );
    }


    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void>
    deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        adminService.deleteUser(
                id,
                authentication
        );

        return ResponseEntity
                .noContent()
                .build();
    }


    // ============================================================
    // LIBRARY
    // ============================================================

    @GetMapping("/library")
    public ResponseEntity<AdminLibraryResponse>
    getLibraryOperations() {

        return ResponseEntity.ok(
                adminService.getLibraryOperations()
        );
    }


    // ============================================================
    // BORROWINGS
    // ============================================================

    @GetMapping("/borrowings")
    public ResponseEntity<List<BorrowResponse>>
    getAllBorrowings() {

        return ResponseEntity.ok(
                adminService.getAllBorrowings()
        );
    }


    @GetMapping("/borrowings/active")
    public ResponseEntity<List<BorrowResponse>>
    getActiveBorrowings() {

        return ResponseEntity.ok(
                adminService.getActiveBorrowings()
        );
    }


    @GetMapping("/borrowings/overdue")
    public ResponseEntity<List<BorrowResponse>>
    getOverdueBorrowings() {

        return ResponseEntity.ok(
                adminService.getOverdueBorrowings()
        );
    }


    // ============================================================
    // RESERVATIONS
    // ============================================================

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>>
    getAllReservations() {

        return ResponseEntity.ok(
                adminService.getAllReservations()
        );
    }


    @GetMapping("/reservations/waiting")
    public ResponseEntity<List<ReservationResponse>>
    getWaitingReservations() {

        return ResponseEntity.ok(
                adminService.getWaitingReservations()
        );
    }


    @GetMapping("/reservations/ready")
    public ResponseEntity<List<ReservationResponse>>
    getReadyReservations() {

        return ResponseEntity.ok(
                adminService.getReadyReservations()
        );
    }


    @PutMapping("/reservations/{id}/status")
    public ResponseEntity<ReservationResponse>
    updateReservationStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {

        return ResponseEntity.ok(
                adminService.updateReservationStatus(
                        id,
                        status
                )
        );
    }


    // ============================================================
    // FINES
    // ============================================================

    @GetMapping("/fines")
    public ResponseEntity<List<FineResponse>>
    getAllFines() {

        return ResponseEntity.ok(
                adminService.getAllFines()
        );
    }


    @GetMapping("/fines/unpaid")
    public ResponseEntity<List<FineResponse>>
    getUnpaidFines() {

        return ResponseEntity.ok(
                adminService.getUnpaidFines()
        );
    }


    @PutMapping("/fines/{id}/pay")
    public ResponseEntity<FineResponse>
    payFine(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminService.payFineAsAdmin(id)
        );
    }
}