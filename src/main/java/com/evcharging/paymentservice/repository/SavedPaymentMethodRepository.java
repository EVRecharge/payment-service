package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {
    List<SavedPaymentMethod> findByUserId(Long userId);
    Optional<SavedPaymentMethod> findByUserIdAndPrimaryTrue(Long userId);
    Optional<SavedPaymentMethod> findByUserIdAndRazorpayTokenId(Long userId, String razorpayTokenId);
    boolean existsByUserId(Long userId);
}
