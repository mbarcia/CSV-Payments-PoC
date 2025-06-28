package com.example.poc.repository;

import com.example.poc.common.domain.PaymentRecord;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentRecordRepository implements PanacheRepository<PaymentRecord> {
}
