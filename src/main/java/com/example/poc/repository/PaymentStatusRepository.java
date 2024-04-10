package com.example.poc.repository;

import com.example.poc.domain.PaymentStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentStatusRepository extends CrudRepository<PaymentStatus, Long> {
}
