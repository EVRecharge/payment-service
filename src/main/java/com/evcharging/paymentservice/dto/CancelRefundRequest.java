package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class CancelRefundRequest {
    /** Share of the booking amount the platform keeps as a cancellation fee (0-100); the rest is refunded. */
    @Min(0)
    @Max(100)
    private int retainPercent;
}
