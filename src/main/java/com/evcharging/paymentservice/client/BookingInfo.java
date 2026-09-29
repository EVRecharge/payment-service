package com.evcharging.paymentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal projection of booking-service's BookingDTO -- only what's needed to price a payment. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingInfo {
    private Long bookingId;
    private Long userId;
    private Long providerId;
    private Double totalCost;
    private String status;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
