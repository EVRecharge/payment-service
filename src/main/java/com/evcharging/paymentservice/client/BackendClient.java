package com.evcharging.paymentservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Looks up booking/user details from booking-service and user-service on behalf of the caller,
 * forwarding the same Authorization header the caller sent to payment-service. This is
 * deliberately the caller's own token (not a service credential): every action here is triggered
 * by a live, authenticated request from the driver themselves, so forwarding their token both
 * satisfies those services' existing ownership checks and avoids minting a separate credential
 * for a case that doesn't need one.
 */
@Component
public class BackendClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public BookingInfo findBooking(Long bookingId, String bearerToken) {
        var response = restTemplate.exchange(
                publicBaseUrl + "/api/bookings/" + bookingId, HttpMethod.GET, authorized(bearerToken), BookingInfo.class);
        return response.getBody();
    }

    public UserContact findUser(Long userId, String bearerToken) {
        var response = restTemplate.exchange(
                publicBaseUrl + "/api/users/" + userId, HttpMethod.GET, authorized(bearerToken), UserContact.class);
        return response.getBody();
    }

    private HttpEntity<Void> authorized(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);
        return new HttpEntity<>(headers);
    }
}
