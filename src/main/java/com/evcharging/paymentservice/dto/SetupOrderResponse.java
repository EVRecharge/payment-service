package com.evcharging.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A tiny (auto-refunded) authorization order used only to tokenize a new card/UPI for later reuse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetupOrderResponse {
    private String razorpayOrderId;
    private long amountPaise;
    private String currency;
    private String razorpayKeyId;
    private String razorpayCustomerId;
}
