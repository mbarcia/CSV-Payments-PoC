package com.example.poc.repository;

import com.example.poc.common.domain.AckPaymentSent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AckPaymentSentRepository implements PanacheRepository<AckPaymentSent> {
    // You can add custom methods here if needed
}

