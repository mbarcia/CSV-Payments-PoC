package com.example.poc.repository;

import com.example.poc.domain.SendPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SendPaymentRepository extends JpaRepository<SendPayment, String> {
    @Override
    Optional<SendPayment> findById(String id);
}
