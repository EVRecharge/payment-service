package com.evcharging.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Ledger of money actually paid: one row per captured booking payment, tying the driver who paid
 * ({@code userId}) to the provider who received it ({@code providerId}). Written the moment a
 * payment is captured, so unlike {@link Transaction} it never holds CREATED/FAILED attempts.
 * Users and providers live in other services' databases, so those two columns are indexed ids
 * rather than foreign keys. Amounts are in paise.
 */
@Entity
@Table(name = "payment_records", indexes = {
    @Index(name = "idx_payment_records_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_records_provider_id", columnList = "provider_id"),
    @Index(name = "idx_payment_records_booking_id", columnList = "booking_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentRecordId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /** The driver who made the payment. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The provider who received it. */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "razorpay_order_id", nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private String currency;

    @Column(name = "provider_amount_paise")
    private Long providerAmountPaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    /** PAID, PARTIALLY_REFUNDED, REFUNDED */
    @Column(nullable = false)
    private String status;

    @Column(name = "refunded_amount_paise")
    private Long refundedAmountPaise;

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
