package com.evcharging.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long bookingId;
    private long amountPaise;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
