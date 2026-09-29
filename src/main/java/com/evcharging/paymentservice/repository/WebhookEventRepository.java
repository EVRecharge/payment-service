package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByRazorpayEventId(String razorpayEventId);
}
