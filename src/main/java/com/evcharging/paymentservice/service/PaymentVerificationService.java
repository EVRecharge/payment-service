package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.dto.VerifyPaymentRequest;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.razorpay.RazorpaySignatureVerifier;
import com.evcharging.paymentservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms a checkout result the browser reports back. The signature check is what actually
 * proves the payment happened -- a client (malicious or buggy) could call this endpoint with a
 * fabricated order/payment id pair, so without verifying the HMAC we'd be trusting the browser to
 * tell the truth about its own payment. The Razorpay webhook (see WebhookService) is the
 * authoritative, server-to-server confirmation and will independently arrive and reconcile the
 * same transaction, so this endpoint only needs to give the driver instant feedback.
 */
@Service
public class PaymentVerificationService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RazorpaySignatureVerifier signatureVerifier;

    @Transactional
    public Transaction verify(VerifyPaymentRequest request, Long callerUserId) {
        Transaction tx = transactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown order"));

        if (!tx.getUserId().equals(callerUserId)) {
            throw new SecurityException("You can only verify your own payment");
        }

        boolean valid = signatureVerifier.verifyCheckoutSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
        if (!valid) {
            tx.setStatus("FAILED");
            tx.setFailureReason("Signature verification failed");
            transactionRepository.save(tx);
            throw new SecurityException("Payment signature could not be verified");
        }

        if (!"CAPTURED".equals(tx.getStatus())) {
            tx.setRazorpayPaymentId(request.getRazorpayPaymentId());
            tx.setStatus("CAPTURED");
            transactionRepository.save(tx);
        }
        return tx;
    }
}
