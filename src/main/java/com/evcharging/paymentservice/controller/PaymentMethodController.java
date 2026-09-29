package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.service.PaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/methods")
public class PaymentMethodController extends BaseAuthenticatedController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest httpRequest) {
        if (!hasRole("USER")) {
            return forbidden("Only drivers have saved payment methods");
        }
        return ResponseEntity.ok(paymentMethodService.listMethods(currentEntityId(), bearerToken(httpRequest)));
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
