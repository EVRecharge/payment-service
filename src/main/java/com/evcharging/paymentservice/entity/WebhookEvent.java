package com.evcharging.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Records every Razorpay webhook event this service has already processed, keyed by Razorpay's own
 * event id. Razorpay retries webhook delivery on timeout, so without this dedupe a payment could
 * otherwise be captured and transferred twice.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_events_event_id", columnList = "razorpay_event_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razorpay_event_id", nullable = false)
    private String razorpayEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;
}
