package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.ProviderPayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderPayoutAccountRepository extends JpaRepository<ProviderPayoutAccount, Long> {
    Optional<ProviderPayoutAccount> findByProviderId(Long providerId);
    Optional<ProviderPayoutAccount> findByRazorpayAccountId(String razorpayAccountId);
}
