package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Sent by booking-service (never by a browser) the moment a provider confirms a booking, carrying
 * the booking's own facts directly rather than asking payment-service to look them up -- there's
 * no live user session to forward a token from at that point, and booking-service already has this
 * data in hand from the row it just updated.
 */
@Data
public class ChargeBookingRequest {
    @NotNull
    private Long bookingId;
    @NotNull
    private Long userId;
    @NotNull
    private Long providerId;
    @NotNull
    private Double amountRupees;
}
