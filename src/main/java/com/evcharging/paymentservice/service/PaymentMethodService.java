package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.client.BackendClient;
import com.evcharging.paymentservice.client.UserContact;
import com.evcharging.paymentservice.dto.PaymentMethodResponse;
import com.evcharging.paymentservice.dto.SetupOrderResponse;
import com.evcharging.paymentservice.dto.VerifySetupRequest;
import com.evcharging.paymentservice.entity.PaymentCustomer;
import com.evcharging.paymentservice.entity.SavedPaymentMethod;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.razorpay.RazorpaySignatureVerifier;
import com.evcharging.paymentservice.repository.PaymentCustomerRepository;
import com.evcharging.paymentservice.repository.SavedPaymentMethodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lets a driver explicitly add a card/UPI (rather than only saving one as a side effect of paying
 * for a booking), designate one as primary, and remove ones they no longer want. Adding a method
 * still has to go through a real Razorpay Checkout once -- card networks require an actual
 * cardholder-present transaction before a card can be tokenized for later silent ("merchant
 * initiated") charges -- so this runs a tiny INR 1 authorization that's refunded immediately after
 * the resulting token is captured. The primary method is what {@link BookingChargeService} charges
 * automatically once a provider confirms a booking.
 */
@Service
public class PaymentMethodService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);
    private static final long SETUP_AMOUNT_PAISE = 100; // INR 1, refunded right after tokenizing

    @Autowired
    private PaymentCustomerRepository paymentCustomerRepository;

    @Autowired
    private SavedPaymentMethodRepository savedPaymentMethodRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private RazorpaySignatureVerifier signatureVerifier;

    @Autowired
    private BackendClient backendClient;

    @Transactional
    public String resolveOrCreateCustomer(Long userId, String bearerToken) {
        return paymentCustomerRepository.findByUserId(userId)
                .map(PaymentCustomer::getRazorpayCustomerId)
                .orElseGet(() -> {
                    UserContact user = backendClient.findUser(userId, bearerToken);
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", user != null ? user.fullName() : ("Driver #" + userId));
                    if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                        body.put("email", user.getEmail());
                    }
                    if (user != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
                        body.put("contact", user.getPhoneNumber());
                    }
                    body.put("fail_existing", "0");
                    Map<String, Object> customer = razorpayClient.createCustomer(body);
                    String customerId = (String) customer.get("id");
                    PaymentCustomer entity = new PaymentCustomer();
                    entity.setUserId(userId);
                    entity.setRazorpayCustomerId(customerId);
                    paymentCustomerRepository.save(entity);
                    return customerId;
                });
    }

    @Transactional
    public SetupOrderResponse createSetupOrder(Long userId, String bearerToken) {
        String customerId = resolveOrCreateCustomer(userId, bearerToken);
        Map<String, Object> body = new HashMap<>();
        body.put("amount", SETUP_AMOUNT_PAISE);
        body.put("currency", "INR");
        body.put("receipt", "method_setup_" + userId + "_" + System.currentTimeMillis());
        body.put("notes", Map.of("purpose", "method_setup", "userId", String.valueOf(userId)));

        Map<String, Object> order = razorpayClient.createOrder(body);
        return new SetupOrderResponse((String) order.get("id"), SETUP_AMOUNT_PAISE, "INR",
                razorpayClient.getKeyId(), customerId);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public PaymentMethodResponse verifySetup(Long userId, VerifySetupRequest request) {
        boolean valid = signatureVerifier.verifyCheckoutSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
        if (!valid) {
            throw new SecurityException("Payment signature could not be verified");
        }

        Map<String, Object> payment = razorpayClient.fetchPayment(request.getRazorpayPaymentId());
        // An uncaptured authorization can't be refunded (and some accounts don't auto-capture), so capture it first.
        if ("authorized".equals(payment.get("status"))) {
            payment = razorpayClient.capturePayment(request.getRazorpayPaymentId(), SETUP_AMOUNT_PAISE, "INR");
        }

        String method = (String) payment.get("method");
        Map<String, Object> card = (Map<String, Object>) payment.get("card");
        String vpa = (String) payment.get("vpa");
        String tokenId = (String) payment.get("token_id");

        // Not every payment response carries the token id -- fall back to the customer's newest saved token.
        if (tokenId == null || tokenId.isBlank()) {
            Map<String, Object> token = findNewestToken(userId, method);
            if (token != null) {
                tokenId = (String) token.get("id");
                if (card == null) {
                    card = (Map<String, Object>) token.get("card");
                }
                if (vpa == null && token.get("vpa") instanceof Map) {
                    vpa = (String) ((Map<String, Object>) token.get("vpa")).get("username");
                }
            }
        }
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("Razorpay returned no reusable token for payment {} (method {}) for user {}",
                    request.getRazorpayPaymentId(), method, userId);
            throw new IllegalStateException(
                    "This payment method doesn't support being saved for reuse. Try a different card.");
        }

        // Saving the same card twice is a no-op, not a duplicate row.
        Optional<SavedPaymentMethod> already = savedPaymentMethodRepository.findByUserIdAndRazorpayTokenId(userId, tokenId);
        if (already.isPresent()) {
            return toResponse(already.get());
        }

        // The INR 1 authorization was only to obtain a reusable token -- refund it immediately.
        // Best-effort: a refund hiccup shouldn't stop the method from being saved.
        try {
            razorpayClient.createRefund(request.getRazorpayPaymentId(), Map.of("speed", "normal"));
        } catch (Exception e) {
            log.warn("Could not auto-refund the payment-method setup charge for user {}: {}", userId, e.getMessage());
        }

        boolean isFirstMethod = !savedPaymentMethodRepository.existsByUserId(userId);

        SavedPaymentMethod entity = new SavedPaymentMethod();
        entity.setUserId(userId);
        entity.setRazorpayTokenId(tokenId);
        entity.setMethod(method);
        entity.setCardLast4(card != null ? (String) card.get("last4") : null);
        entity.setCardNetwork(card != null ? (String) card.get("network") : null);
        entity.setUpiVpaMasked("upi".equals(method) ? maskVpa(vpa) : null);
        entity.setPrimary(isFirstMethod);
        savedPaymentMethodRepository.save(entity);

        return toResponse(entity);
    }

    /** The most recently created token on the user's Razorpay customer for this payment method, if any. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findNewestToken(Long userId, String method) {
        Optional<String> customerId = getCustomerId(userId);
        if (customerId.isEmpty()) {
            return null;
        }
        try {
            return razorpayClient.listCustomerTokens(customerId.get()).stream()
                    .filter(t -> method == null || method.equals(t.get("method")))
                    .max(Comparator.comparingLong(t -> t.get("created_at") instanceof Number
                            ? ((Number) t.get("created_at")).longValue() : 0L))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not list Razorpay tokens for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<PaymentMethodResponse> listMethods(Long userId) {
        return savedPaymentMethodRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setPrimary(Long userId, String tokenId) {
        List<SavedPaymentMethod> methods = savedPaymentMethodRepository.findByUserId(userId);
        boolean found = methods.stream().anyMatch(m -> m.getRazorpayTokenId().equals(tokenId));
        if (!found) {
            throw new IllegalArgumentException("Payment method not found");
        }
        methods.forEach(m -> m.setPrimary(m.getRazorpayTokenId().equals(tokenId)));
        savedPaymentMethodRepository.saveAll(methods);
    }

    @Transactional
    public void deleteMethod(Long userId, String tokenId) {
        SavedPaymentMethod entity = savedPaymentMethodRepository.findByUserIdAndRazorpayTokenId(userId, tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        paymentCustomerRepository.findByUserId(userId).ifPresent(customer -> {
            try {
                razorpayClient.deleteCustomerToken(customer.getRazorpayCustomerId(), tokenId);
            } catch (Exception e) {
                log.warn("Could not delete Razorpay token {} for user {}: {}", tokenId, userId, e.getMessage());
            }
        });

        boolean wasPrimary = entity.isPrimary();
        savedPaymentMethodRepository.delete(entity);

        if (wasPrimary) {
            savedPaymentMethodRepository.findByUserId(userId).stream().findFirst().ifPresent(next -> {
                next.setPrimary(true);
                savedPaymentMethodRepository.save(next);
            });
        }
    }

    /** The method {@link BookingChargeService} should silently charge once a booking is confirmed. */
    public Optional<SavedPaymentMethod> getPrimaryMethod(Long userId) {
        return savedPaymentMethodRepository.findByUserIdAndPrimaryTrue(userId);
    }

    public Optional<String> getCustomerId(Long userId) {
        return paymentCustomerRepository.findByUserId(userId).map(PaymentCustomer::getRazorpayCustomerId);
    }

    private PaymentMethodResponse toResponse(SavedPaymentMethod m) {
        return new PaymentMethodResponse(
                m.getRazorpayTokenId(), m.getMethod(), m.getCardLast4(), m.getCardNetwork(), m.getUpiVpaMasked(),
                m.isPrimary());
    }

    private String maskVpa(String vpa) {
        if (vpa == null || !vpa.contains("@")) {
            return vpa;
        }
        String[] parts = vpa.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? local : local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
