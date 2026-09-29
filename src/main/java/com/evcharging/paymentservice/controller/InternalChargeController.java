package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.dto.ChargeBookingRequest;
import com.evcharging.paymentservice.service.BookingChargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * Called only by booking-service (using its internal service token -- see
 * booking-service's InternalServiceTokenProvider) the moment a provider confirms a booking. Never
 * exposed to the frontend: a real user JWT has role USER/PROVIDER, never SERVICE, so this is
 * unreachable from the browser regardless of what a client tries to send.
 */
@RestController
@RequestMapping("/api/payments/internal")
public class InternalChargeController extends BaseAuthenticatedController {

    @Autowired
    private BookingChargeService bookingChargeService;

    @PostMapping("/bookings/{bookingId}/charge")
    public ResponseEntity<?> charge(@Valid @RequestBody ChargeBookingRequest request) {
        if (!hasRole("SERVICE")) {
            return forbidden("Internal endpoint");
        }
        try {
            bookingChargeService.chargeOnConfirmation(request);
            return ResponseEntity.ok(Map.of("message", "Charge processed"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
