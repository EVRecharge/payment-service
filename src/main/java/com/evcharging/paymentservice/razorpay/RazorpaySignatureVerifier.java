package com.evcharging.paymentservice.razorpay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies Razorpay's HMAC-SHA256 signatures. There are two distinct secrets/payloads Razorpay
 * signs, and both must be checked -- never trust a "payment succeeded" claim from the browser
 * alone, since a malicious client could call the verify endpoint with a fabricated payload:
 *
 * <ul>
 *   <li>Checkout callback: {@code hmac_sha256(order_id + "|" + payment_id, key_secret)}</li>
 *   <li>Webhook delivery: {@code hmac_sha256(raw_request_body, webhook_secret)}</li>
 * </ul>
 */
@Component
public class RazorpaySignatureVerifier {

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;

    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        return verify(orderId + "|" + paymentId, signature, keySecret);
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        return verify(rawBody, signature, webhookSecret);
    }

    private boolean verify(String payload, String signature, String secret) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
