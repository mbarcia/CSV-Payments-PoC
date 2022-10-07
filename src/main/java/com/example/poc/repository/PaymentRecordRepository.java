package com.example.poc.repository;

import com.example.poc.biz.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    @Override
    Optional<PaymentRecord> findById(Long aLong);
}