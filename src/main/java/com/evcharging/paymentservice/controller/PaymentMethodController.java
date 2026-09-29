package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.dto.VerifySetupRequest;
import com.evcharging.paymentservice.service.PaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/methods")
public class PaymentMethodController extends BaseAuthenticatedController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<?> list() {
        if (!hasRole("USER")) {
            return forbidden("Only drivers have saved payment methods");
        }
        return ResponseEntity.ok(paymentMethodService.listMethods(currentEntityId()));
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(HttpServletRequest httpRequest) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers can add a payment method");
        }
        return ResponseEntity.ok(paymentMethodService.createSetupOrder(currentEntityId(), bearerToken(httpRequest)));
    }

    @PostMapping("/setup/verify")
    public ResponseEntity<?> verifySetup(@Valid @RequestBody VerifySetupRequest request) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers can add a payment method");
        }
        try {
            return ResponseEntity.ok(paymentMethodService.verifySetup(currentEntityId(), request));
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{tokenId}/primary")
    public ResponseEntity<?> setPrimary(@PathVariable String tokenId) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers have saved payment methods");
        }
        try {
            paymentMethodService.setPrimary(currentEntityId(), tokenId);
            return ResponseEntity.ok(Map.of("message", "Primary payment method updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<?> delete(@PathVariable String tokenId) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers have saved payment methods");
        }
        try {
            paymentMethodService.deleteMethod(currentEntityId(), tokenId);
            return ResponseEntity.ok(Map.of("message", "Payment method removed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
