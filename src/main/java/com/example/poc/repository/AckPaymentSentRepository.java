package com.example.poc.repository;

import com.example.poc.domain.AckPaymentSent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AckPaymentSentRepository extends CrudRepository<AckPaymentSent, Long> {
}
