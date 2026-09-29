package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.PaymentCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentCustomerRepository extends JpaRepository<PaymentCustomer, Long> {
    Optional<PaymentCustomer> findByUserId(Long userId);
}
