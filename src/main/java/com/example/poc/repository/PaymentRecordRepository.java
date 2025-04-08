package com.example.poc.repository;

import com.example.poc.domain.PaymentRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends CrudRepository<PaymentRecord, UUID> {
}
