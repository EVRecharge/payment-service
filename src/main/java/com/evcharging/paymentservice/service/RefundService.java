package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Reverses a captured payment (e.g. a cancelled booking). Reversing transfers too, when there were any, is what {@code reverse_all} does. */
@Service
public class RefundService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RazorpayClient razorpayClient;

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
        return transactionRepository.save(tx);
    }
}
