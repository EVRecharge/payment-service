package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/refunds")
public class RefundController extends BaseAuthenticatedController {

    @Autowired
    private RefundService refundService;

    @PostMapping("/{bookingId}")
    public ResponseEntity<?> refund(@PathVariable Long bookingId) {
        boolean isProvider = hasRole("PROVIDER");
        if (!isProvider && !hasRole("USER")) {
            return forbidden("Not authorized");
        }
        try {
            Transaction tx = refundService.refund(bookingId, currentEntityId(), isProvider);
            return ResponseEntity.ok(Map.of("status", tx.getStatus(), "razorpayRefundId", tx.getRazorpayRefundId()));
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
