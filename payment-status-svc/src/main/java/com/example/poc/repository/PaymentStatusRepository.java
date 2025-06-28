package com.example.poc.repository;

import com.example.poc.common.domain.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentStatusRepository implements PanacheRepository<PaymentStatus> {
}
