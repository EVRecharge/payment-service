package com.evcharging.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long bookingId;
    private String razorpayOrderId;
    private long amountPaise;
    private String currency;
    private String razorpayKeyId;
    private String razorpayCustomerId;
}
