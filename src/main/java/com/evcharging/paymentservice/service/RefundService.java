package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/** Reverses a captured payment (e.g. a cancelled booking). Reversing transfers too, when there were any, is what {@code reverse_all} does. */
@Service
public class RefundService {

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    /**
     * Refunds a cancelled-after-confirmation booking, keeping {@code retainPercent} of the amount as
     * the cancellation fee (0 = full refund). Called by booking-service, so there's no caller to
     * authorize here. A booking that was never actually charged has nothing to refund.
     */
    @Transactional
    public Optional<Transaction> refundForCancellation(Long bookingId, int retainPercent) {
        Optional<Transaction> found = transactionRepository.findByBookingId(bookingId);
        if (found.isEmpty() || !"CAPTURED".equals(found.get().getStatus())) {
            return Optional.empty();
        }
        Transaction tx = found.get();
        long retained = Math.round(tx.getAmountPaise() * (retainPercent / 100.0));
        long refundPaise = tx.getAmountPaise() - retained;
        if (refundPaise <= 0) {
            return Optional.of(tx);
        }

        Map<String, Object> body = Map.of(
                "amount", refundPaise,
                "speed", "normal",
                "reverse_all", tx.getProviderAccountId() != null ? 1 : 0);
        Map<String, Object> refund = razorpayClient.createRefund(tx.getRazorpayPaymentId(), body);

        tx.setStatus(retained > 0 ? "PARTIALLY_REFUNDED" : "REFUNDED");
        tx.setRazorpayRefundId((String) refund.get("id"));
        tx.setRefundedAmountPaise(refundPaise);
        Transaction saved = transactionRepository.save(tx);
        paymentRecordService.recordRefund(saved);
        return Optional.of(saved);
    }

    @Transactional
    public Transaction refund(Long bookingId, Long callerEntityId, boolean callerIsProvider) {
        Transaction tx = transactionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for this booking"));

        boolean owns = callerIsProvider ? tx.getProviderId().equals(callerEntityId) : tx.getUserId().equals(callerEntityId);
        if (!owns) {
            throw new SecurityException("You can only refund your own booking's payment");
        }
        if (!"CAPTURED".equals(tx.getStatus())) {
            throw new IllegalStateException("Only a captured payment can be refunded (status: " + tx.getStatus() + ")");
        }

        Map<String, Object> body = Map.of(
                "amount", tx.getAmountPaise(),
                "speed", "normal",
                "reverse_all", tx.getProviderAccountId() != null ? 1 : 0);
        Map<String, Object> refund = razorpayClient.createRefund(tx.getRazorpayPaymentId(), body);

        tx.setStatus("REFUNDED");
        tx.setRazorpayRefundId((String) refund.get("id"));
        tx.setRefundedAmountPaise(tx.getAmountPaise());
        Transaction saved = transactionRepository.save(tx);
        paymentRecordService.recordRefund(saved);
        return saved;
    }
}
