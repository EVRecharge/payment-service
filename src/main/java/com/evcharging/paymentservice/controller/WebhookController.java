package com.evcharging.paymentservice.controller;

import com.evcharging.paymentservice.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint Razorpay calls directly (no JWT -- see SecurityConfig). Authenticity is proven
 * by the {@code X-Razorpay-Signature} header instead, checked against the raw request body inside
 * WebhookService; requests that fail that check are rejected before anything is applied.
 */
@RestController
@RequestMapping("/api/payments/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private WebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<?> handle(@org.springframework.web.bind.annotation.RequestBody String rawBody,
                                     @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        try {
            webhookService.handle(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            return ResponseEntity.status(400).build();
        }
    }
}
