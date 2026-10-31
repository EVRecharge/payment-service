package com.evcharging.paymentservice.repository;

import com.evcharging.paymentservice.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByTransactionTransactionId(Long transactionId);
    List<PaymentRecord> findByUserIdOrderByPaidAtDesc(Long userId);
    List<PaymentRecord> findByProviderIdOrderByPaidAtDesc(Long providerId);
}
