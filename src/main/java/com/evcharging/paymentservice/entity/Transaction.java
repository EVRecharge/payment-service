package com.evcharging.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per booking payment. Amounts are stored in paise (the smallest INR unit, matching
 * Razorpay's convention) to avoid floating-point rounding errors. The server -- never the client
 * -- computes {@code amountPaise} from the booking's real cost, so a tampered client request
 * cannot pay less than what is owed.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_booking_id", columnList = "booking_id", unique = true),
    @Index(name = "idx_transactions_user_id", columnList = "user_id"),
    @Index(name = "idx_transactions_provider_id", columnList = "provider_id"),
    @Index(name = "idx_transactions_razorpay_order_id", columnList = "razorpay_order_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "razorpay_order_id", nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private String currency;

    @Column(name = "provider_amount_paise")
    private Long providerAmountPaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "provider_account_id")
    private String providerAccountId;

    /** CREATED, CAPTURED, FAILED, REFUNDED, PARTIALLY_REFUNDED */
    @Column(nullable = false)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "razorpay_refund_id")
    private String razorpayRefundId;

    @Column(name = "refunded_amount_paise")
    private Long refundedAmountPaise;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
