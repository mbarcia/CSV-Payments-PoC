package com.example.poc.repository;

import com.example.poc.domain.AckPaymentSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AckPaymentSentRepository extends JpaRepository<AckPaymentSent, Long> {
}
