package com.example.poc.repository;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AckPaymentSentRepository extends JpaRepository<AckPaymentSent, Long> {
    @Override
    Optional<AckPaymentSent> findById(Long id);

    Optional<AckPaymentSent> findByRecord(PaymentRecord record);
}
