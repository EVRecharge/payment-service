package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifySetupRequest {
    @NotBlank
    private String razorpayOrderId;
    @NotBlank
    private String razorpayPaymentId;
    @NotBlank
    private String razorpaySignature;

    /** Simulated setup only: what was entered, reduced to what is safe to store (never the full number or CVV). */
    private String mockMethod;
    private String mockCardLast4;
    private String mockCardNetwork;
    private String mockVpa;
}
