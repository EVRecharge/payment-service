package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.dto.CreateOrderRequest;
import com.evcharging.paymentservice.dto.OrderResponse;
import com.evcharging.paymentservice.dto.VerifyPaymentRequest;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.service.OrderService;
import com.evcharging.paymentservice.service.PaymentVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/orders")
public class OrderController extends BaseAuthenticatedController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentVerificationService paymentVerificationService;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers can pay for a booking");
        }
        try {
            OrderResponse response = orderService.createOrder(
                    request.getBookingId(), currentEntityId(), bearerToken(httpRequest));
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers can verify a payment");
        }
        try {
            Transaction tx = paymentVerificationService.verify(request, currentEntityId());
            return ResponseEntity.ok(Map.of("status", tx.getStatus(), "bookingId", tx.getBookingId()));
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
