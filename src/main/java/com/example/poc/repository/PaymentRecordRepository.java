package com.example.poc.repository;

import com.example.poc.domain.PaymentRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRecordRepository extends CrudRepository<PaymentRecord, Long> {
}
