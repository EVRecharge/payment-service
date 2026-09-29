package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.entity.ProviderPayoutAccount;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.entity.WebhookEvent;
import com.evcharging.paymentservice.razorpay.RazorpaySignatureVerifier;
import com.evcharging.paymentservice.repository.ProviderPayoutAccountRepository;
import com.evcharging.paymentservice.repository.TransactionRepository;
import com.evcharging.paymentservice.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * The authoritative confirmation path for payment/account status: unlike the browser-driven
 * verify endpoint, this is Razorpay calling us directly, so it's what reconciliation should trust
 * even if a driver's browser crashed mid-checkout or a client tried to lie about the outcome.
 * Every event is deduped by Razorpay's own event id before being applied, since Razorpay retries
 * delivery on timeout and this handler must stay idempotent.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private RazorpaySignatureVerifier signatureVerifier;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProviderPayoutAccountRepository payoutAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handle(String rawBody, String signatureHeader) {
        if (!signatureVerifier.verifyWebhookSignature(rawBody, signatureHeader)) {
            throw new SecurityException("Webhook signature verification failed");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("Could not parse Razorpay webhook payload: {}", e.getMessage());
            return;
        }

        String eventId = root.path("id").asText(null);
        String eventType = root.path("event").asText(null);
        if (eventId == null || eventType == null) {
            return;
        }
        if (webhookEventRepository.existsByRazorpayEventId(eventId)) {
            log.info("Ignoring already-processed webhook event {}", eventId);
            return;
        }

        if ("payment.captured".equals(eventType)) {
            handlePaymentCaptured(root);
        } else if ("payment.failed".equals(eventType)) {
            handlePaymentFailed(root);
        } else if ("account.activated".equals(eventType) || "account.under_review".equals(eventType)
                || "account.needs_clarification".equals(eventType) || "account.rejected".equals(eventType)) {
            handleAccountStatus(root, eventType);
        } else {
            log.debug("Ignoring unhandled Razorpay webhook event type {}", eventType);
        }

        WebhookEvent record = new WebhookEvent();
        record.setRazorpayEventId(eventId);
        record.setEventType(eventType);
        webhookEventRepository.save(record);
    }

    private void handlePaymentCaptured(JsonNode root) {
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asText(null);
        String paymentId = payment.path("id").asText(null);
        if (orderId == null) {
            return;
        }
        transactionRepository.findByRazorpayOrderId(orderId).ifPresent(tx -> {
            tx.setRazorpayPaymentId(paymentId);
            tx.setStatus("CAPTURED");
            transactionRepository.save(tx);
        });
    }

    private void handlePaymentFailed(JsonNode root) {
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asText(null);
        String reason = payment.path("error_description").asText("Payment failed");
        if (orderId == null) {
            return;
        }
        transactionRepository.findByRazorpayOrderId(orderId).ifPresent(tx -> {
            if (!"CAPTURED".equals(tx.getStatus())) {
                tx.setStatus("FAILED");
                tx.setFailureReason(reason);
                transactionRepository.save(tx);
            }
        });
    }

    private void handleAccountStatus(JsonNode root, String eventType) {
        JsonNode account = root.path("payload").path("account").path("entity");
        String accountId = account.path("id").asText(null);
        if (accountId == null) {
            return;
        }
        payoutAccountRepository.findByRazorpayAccountId(accountId).ifPresent(payoutAccount -> {
            String status;
            if ("account.activated".equals(eventType)) {
                status = "ACTIVATED";
            } else if ("account.needs_clarification".equals(eventType)) {
                status = "NEEDS_CLARIFICATION";
            } else if ("account.rejected".equals(eventType)) {
                status = "REJECTED";
            } else {
                status = "CREATED";
            }
            payoutAccount.setStatus(status);
            payoutAccount.setStatusReason("Updated by Razorpay webhook: " + eventType);
            payoutAccountRepository.save(payoutAccount);
        });
    }
}
