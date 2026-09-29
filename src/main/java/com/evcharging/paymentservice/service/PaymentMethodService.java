package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.client.BackendClient;
import com.evcharging.paymentservice.client.UserContact;
import com.evcharging.paymentservice.dto.PaymentMethodResponse;
import com.evcharging.paymentservice.entity.PaymentCustomer;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.repository.PaymentCustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lists/removes a driver's saved cards and UPI IDs. These are never stored here as raw numbers --
 * Razorpay's hosted Checkout is what actually collects card details and "tokenizes" them against a
 * Razorpay Customer id (created lazily, on first use); this service only keeps that customer id
 * and always reads the live token list from Razorpay, so there's nothing sensitive to keep in sync
 * or leak from this database.
 */
@Service
public class PaymentMethodService {

    @Autowired
    private PaymentCustomerRepository paymentCustomerRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private BackendClient backendClient;

    @Transactional
    public String resolveOrCreateCustomer(Long userId, String bearerToken) {
        return paymentCustomerRepository.findByUserId(userId)
                .map(PaymentCustomer::getRazorpayCustomerId)
                .orElseGet(() -> {
                    UserContact user = backendClient.findUser(userId, bearerToken);
                    Map<String, Object> body = Map.of(
                            "name", user != null ? user.fullName() : ("Driver #" + userId),
                            "email", user != null && user.getEmail() != null ? user.getEmail() : "",
                            "contact", user != null && user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                            "fail_existing", "0");
                    Map<String, Object> customer = razorpayClient.createCustomer(body);
                    String customerId = (String) customer.get("id");
                    PaymentCustomer entity = new PaymentCustomer();
                    entity.setUserId(userId);
                    entity.setRazorpayCustomerId(customerId);
                    paymentCustomerRepository.save(entity);
                    return customerId;
                });
    }

    public List<PaymentMethodResponse> listMethods(Long userId, String bearerToken) {
        return paymentCustomerRepository.findByUserId(userId)
                .map(c -> mapTokens(razorpayClient.listCustomerTokens(c.getRazorpayCustomerId())))
                .orElse(List.of());
    }

    public void deleteMethod(Long userId, String tokenId) {
        PaymentCustomer customer = paymentCustomerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No saved payment methods"));
        razorpayClient.deleteCustomerToken(customer.getRazorpayCustomerId(), tokenId);
    }

    @SuppressWarnings("unchecked")
    private List<PaymentMethodResponse> mapTokens(List<Map<String, Object>> tokens) {
        return tokens.stream().map(t -> {
            String method = (String) t.get("method");
            Map<String, Object> card = (Map<String, Object>) t.get("card");
            Map<String, Object> vpa = (Map<String, Object>) t.get("vpa");
            String cardLast4 = card != null ? (String) card.get("last4") : null;
            String cardNetwork = card != null ? (String) card.get("network") : null;
            String vpaMasked = vpa != null ? maskVpa((String) vpa.get("address")) : null;
            return new PaymentMethodResponse((String) t.get("id"), method, cardLast4, cardNetwork, vpaMasked);
        }).collect(Collectors.toList());
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
