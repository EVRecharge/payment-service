package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.dto.TransactionResponse;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/transactions")
public class TransactionController extends BaseAuthenticatedController {

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> byBooking(@PathVariable Long bookingId) {
        Optional<Transaction> tx = transactionRepository.findByBookingId(bookingId)
                .filter(t -> t.getUserId().equals(currentEntityId()) || t.getProviderId().equals(currentEntityId()));
        if (tx.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No payment found for this booking"));
        }
        return ResponseEntity.ok(toResponse(tx.get()));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(tx.getBookingId(), tx.getAmountPaise(), tx.getCurrency(), tx.getStatus(), tx.getCreatedAt());
    }
}
