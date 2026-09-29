package com.evcharging.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * A provider's Razorpay Route "linked account" -- the destination for their share of each booking
 * payment. Only the last 4 digits of the bank account and PAN are retained locally; the full
 * numbers live only in Razorpay's KYC systems, never in this database.
 */
@Entity
@Table(name = "provider_payout_accounts", indexes = {
    @Index(name = "idx_payout_accounts_provider_id", columnList = "provider_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "razorpay_account_id")
    private String razorpayAccountId;

    /** CREATED, ACTIVATED, NEEDS_CLARIFICATION, REJECTED, SUSPENDED */
    @Column(nullable = false)
    private String status;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "bank_account_last4")
    private String bankAccountLast4;

    @Column
    private String ifsc;

    @Column(name = "pan_last4")
    private String panLast4;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "status_reason")
    private String statusReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
