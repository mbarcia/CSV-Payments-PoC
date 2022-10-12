package com.example.poc.repository;

import com.example.poc.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, Long> {
    @Override
    Optional<PaymentStatus> findById(Long id);

}
