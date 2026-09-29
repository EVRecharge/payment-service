package com.evcharging.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * A driver's saved card/UPI, tracked locally only for its Razorpay token id, display metadata
 * (last 4, network) and which one is primary -- the actual card/bank data lives only in Razorpay,
 * reached exclusively through {@code razorpayTokenId}. The primary method is what
 * {@link com.evcharging.paymentservice.service.BookingChargeService} charges automatically once a
 * provider confirms a booking.
 */
@Entity
@Table(name = "saved_payment_methods", indexes = {
    @Index(name = "idx_saved_methods_user_id", columnList = "user_id"),
    @Index(name = "idx_saved_methods_token_id", columnList = "razorpay_token_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "razorpay_token_id", nullable = false)
    private String razorpayTokenId;

    @Column(nullable = false)
    private String method;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_network")
    private String cardNetwork;

    @Column(name = "upi_vpa_masked")
    private String upiVpaMasked;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
