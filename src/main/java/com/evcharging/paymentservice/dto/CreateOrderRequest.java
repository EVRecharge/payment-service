package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateOrderRequest {
    @NotNull
    private Long bookingId;
}
