package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.repository.ProviderPayoutAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Shared by {@link OrderService} and {@link BookingChargeService} so both orders split provider payouts identically. */
@Component
public class TransferCalculator {

    @Autowired
    private ProviderPayoutAccountRepository payoutAccountRepository;

    @Value("${app.razorpay.provider-split-percent}")
    private int providerSplitPercent;

    public Optional<Transfer> computeTransfer(Long providerId, long amountPaise) {
        return payoutAccountRepository.findByProviderId(providerId)
                .filter(a -> "ACTIVATED".equals(a.getStatus()))
                .map(a -> {
                    long providerAmountPaise = Math.round(amountPaise * (providerSplitPercent / 100.0));
                    return new Transfer(a.getRazorpayAccountId(), providerAmountPaise, amountPaise - providerAmountPaise);
                });
    }

    public static class Transfer {
        private final String razorpayAccountId;
        private final long providerAmountPaise;
        private final long platformFeePaise;

        Transfer(String razorpayAccountId, long providerAmountPaise, long platformFeePaise) {
            this.razorpayAccountId = razorpayAccountId;
            this.providerAmountPaise = providerAmountPaise;
            this.platformFeePaise = platformFeePaise;
        }

        public String getRazorpayAccountId() { return razorpayAccountId; }
        public long getProviderAmountPaise() { return providerAmountPaise; }
        public long getPlatformFeePaise() { return platformFeePaise; }
    }
}
