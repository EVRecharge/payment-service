package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.dto.PayoutAccountRequest;
import com.evcharging.paymentservice.dto.PayoutAccountResponse;
import com.evcharging.paymentservice.entity.ProviderPayoutAccount;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.repository.ProviderPayoutAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Onboards a charging-station provider onto Razorpay Route so their share of each booking payment
 * settles straight to their own bank account. This calls three real Razorpay APIs in sequence
 * (create linked account -> add the beneficiary/KYC stakeholder -> request the "route" product),
 * but Razorpay only actually activates the account after reviewing the submitted KYC documents --
 * that review, and the platform's own Route approval, happen on Razorpay's side and can't be
 * completed from here. Until an account reaches ACTIVATED, {@link OrderService} simply doesn't
 * split payments for that provider and the platform holds the full amount.
 */
@Service
public class ProviderPayoutService {

    @Autowired
    private ProviderPayoutAccountRepository repository;

    @Autowired
    private RazorpayClient razorpayClient;

    public PayoutAccountResponse getStatus(Long providerId) {
        return repository.findByProviderId(providerId)
                .map(this::toResponse)
                .orElseGet(() -> new PayoutAccountResponse("NOT_STARTED", null, null, null, null, null));
    }

    @Transactional
    public PayoutAccountResponse submit(Long providerId, PayoutAccountRequest request) {
        ProviderPayoutAccount existing = repository.findByProviderId(providerId).orElse(null);
        if (existing != null && List.of("CREATED", "ACTIVATED").contains(existing.getStatus())) {
            throw new IllegalStateException("A payout account already exists for this provider (status: "
                    + existing.getStatus() + ")");
        }

        Map<String, Object> accountBody = Map.of(
                "email", request.getEmail(),
                "phone", request.getPhone(),
                "type", "route",
                "legal_business_name", request.getBeneficiaryName(),
                "business_type", request.getBusinessType(),
                "contact_name", request.getBeneficiaryName(),
                "profile", Map.of(
                        "category", "utilities",
                        "subcategory", "electricity",
                        "addresses", Map.of()));
        Map<String, Object> account = razorpayClient.createRouteAccount(accountBody);
        String accountId = (String) account.get("id");

        Map<String, Object> stakeholderBody = Map.of(
                "name", request.getBeneficiaryName(),
                "email", request.getEmail(),
                "kyc", Map.of("pan", request.getPan()));
        razorpayClient.createStakeholder(accountId, stakeholderBody);

        Map<String, Object> productBody = Map.of(
                "product_name", "route",
                "tnc_accepted", true,
                "settlements", Map.of(
                        "account_number", request.getAccountNumber(),
                        "ifsc_code", request.getIfsc(),
                        "beneficiary_name", request.getBeneficiaryName()));
        razorpayClient.requestRouteProduct(accountId, productBody);

        ProviderPayoutAccount entity = existing != null ? existing : new ProviderPayoutAccount();
        entity.setProviderId(providerId);
        entity.setRazorpayAccountId(accountId);
        entity.setStatus("CREATED");
        entity.setBeneficiaryName(request.getBeneficiaryName());
        entity.setBankAccountLast4(last4(request.getAccountNumber()));
        entity.setIfsc(request.getIfsc());
        entity.setPanLast4(last4(request.getPan()));
        entity.setBusinessType(request.getBusinessType());
        entity.setStatusReason("Submitted -- awaiting Razorpay KYC review");
        repository.save(entity);

        return toResponse(entity);
    }

    private String last4(String value) {
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private PayoutAccountResponse toResponse(ProviderPayoutAccount a) {
        return new PayoutAccountResponse(
                a.getStatus(), a.getBeneficiaryName(), a.getBankAccountLast4(), a.getIfsc(), a.getPanLast4(),
                a.getStatusReason());
    }
}
