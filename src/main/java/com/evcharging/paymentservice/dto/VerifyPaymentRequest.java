package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifyPaymentRequest {
    @NotBlank
    private String razorpayOrderId;
    @NotBlank
    private String razorpayPaymentId;
    @NotBlank
    private String razorpaySignature;
}
