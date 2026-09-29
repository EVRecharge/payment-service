package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.dto.PayoutAccountRequest;
import com.evcharging.paymentservice.service.ProviderPayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/provider-accounts")
public class ProviderPayoutController extends BaseAuthenticatedController {

    @Autowired
    private ProviderPayoutService providerPayoutService;

    @GetMapping("/me")
    public ResponseEntity<?> myStatus() {
        if (!hasRole("PROVIDER")) {
            return forbidden("Only charging stations have a payout account");
        }
        return ResponseEntity.ok(providerPayoutService.getStatus(currentEntityId()));
    }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody PayoutAccountRequest request) {
        if (!hasRole("PROVIDER")) {
            return forbidden("Only charging stations can submit a payout account");
        }
        try {
            return ResponseEntity.ok(providerPayoutService.submit(currentEntityId(), request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error",
                    "Razorpay rejected the payout account submission: " + e.getMessage()));
        }
    }
}
