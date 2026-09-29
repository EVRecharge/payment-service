package com.evcharging.paymentservice.razorpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Razorpay's plain REST API (HTTP Basic auth with key id/secret) -- no vendor
 * SDK dependency, just the endpoints this service actually needs. All calls go over HTTPS; the key
 * secret never leaves this service (it's read from Secrets Manager via env, see application.yml).
 */
@Component
public class RazorpayClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.api-base-url}")
    private String apiBaseUrl;

    public String getKeyId() {
        return keyId;
    }

    public Map<String, Object> createOrder(Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v1/orders", body);
    }

    public Map<String, Object> createCustomer(Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v1/customers", body);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listCustomerTokens(String customerId) {
        Map<String, Object> response = exchange(HttpMethod.GET, "/v1/customers/" + customerId + "/tokens", null);
        Object items = response.get("items");
        return items instanceof List ? (List<Map<String, Object>>) items : List.of();
    }

    public void deleteCustomerToken(String customerId, String tokenId) {
        exchange(HttpMethod.DELETE, "/v1/customers/" + customerId + "/tokens/" + tokenId, null);
    }

    public Map<String, Object> createRefund(String paymentId, Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v1/payments/" + paymentId + "/refund", body);
    }

    public Map<String, Object> createRouteAccount(Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v2/accounts", body);
    }

    public Map<String, Object> createStakeholder(String accountId, Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v2/accounts/" + accountId + "/stakeholders", body);
    }

    public Map<String, Object> requestRouteProduct(String accountId, Map<String, Object> body) {
        return exchange(HttpMethod.POST, "/v2/accounts/" + accountId + "/products", body);
    }

    public Map<String, Object> fetchAccount(String accountId) {
        return exchange(HttpMethod.GET, "/v2/accounts/" + accountId, null);
    }

    private Map<String, Object> exchange(HttpMethod method, String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(keyId, keySecret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                apiBaseUrl + path, method, entity, new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
    }
}
