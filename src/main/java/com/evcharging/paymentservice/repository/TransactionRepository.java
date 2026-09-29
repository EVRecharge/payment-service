package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByBookingId(Long bookingId);
    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByProviderId(Long providerId);
}
