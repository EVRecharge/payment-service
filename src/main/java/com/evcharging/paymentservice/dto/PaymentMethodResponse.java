package com.evcharging.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private String tokenId;
    /** "card" or "vpa" (UPI) -- whatever Razorpay's token method field reports. */
    private String method;
    private String cardLast4;
    private String cardNetwork;
    private String upiVpaMasked;
}
