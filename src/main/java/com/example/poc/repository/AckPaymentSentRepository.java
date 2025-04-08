package com.example.poc.repository;

import com.example.poc.domain.AckPaymentSent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AckPaymentSentRepository extends CrudRepository<AckPaymentSent, UUID> {
}
