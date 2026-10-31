package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.entity.PaymentRecord;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps the {@link PaymentRecord} ledger in step with {@link Transaction}s as they are captured or refunded. */
@Service
public class PaymentRecordService {

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    /** Idempotent: capture can be reported by the browser, the webhook and the auto-charge, in any order. */
    @Transactional
    public PaymentRecord recordPayment(Transaction tx) {
        PaymentRecord record = paymentRecordRepository.findByTransactionTransactionId(tx.getTransactionId())
                .orElseGet(PaymentRecord::new);
        record.setTransaction(tx);
        record.setBookingId(tx.getBookingId());
        record.setUserId(tx.getUserId());
        record.setProviderId(tx.getProviderId());
        record.setRazorpayOrderId(tx.getRazorpayOrderId());
        if (tx.getRazorpayPaymentId() != null) {
            record.setRazorpayPaymentId(tx.getRazorpayPaymentId());
        }
        record.setAmountPaise(tx.getAmountPaise());
        record.setCurrency(tx.getCurrency());
        record.setProviderAmountPaise(tx.getProviderAmountPaise());
        record.setPlatformFeePaise(tx.getPlatformFeePaise());
        if (record.getStatus() == null) {
            record.setStatus("PAID");
        }
        return paymentRecordRepository.save(record);
    }

    /** Mirrors a refund onto the ledger row (creating it first if the payment was never recorded). */
    @Transactional
    public void recordRefund(Transaction tx) {
        PaymentRecord record = recordPayment(tx);
        record.setStatus(tx.getStatus());
        record.setRefundedAmountPaise(tx.getRefundedAmountPaise());
        paymentRecordRepository.save(record);
    }
}
