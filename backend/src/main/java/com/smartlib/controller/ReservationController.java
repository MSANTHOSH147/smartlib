package com.smartlib.controller;

import com.smartlib.dto.reservation.ReservationRequest;
import com.smartlib.dto.reservation.ReservationResponse;
import com.smartlib.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reservationService.createReservation(
                                request,
                                authentication
                        )
                );
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>>
    getMyReservations(
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.getMyReservations(
                        authentication
                )
        );
    }

    @GetMapping("/my/active")
    public ResponseEntity<List<ReservationResponse>>
    getMyActiveReservations(
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.getMyActiveReservations(
                        authentication
                )
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse>
    cancelReservation(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.cancelReservation(
                        id,
                        authentication
                )
        );
    }
}