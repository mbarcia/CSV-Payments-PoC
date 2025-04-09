package com.example.poc.service;

import com.example.poc.command.ProcessPaymentStatusCommand;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessPaymentStatusService extends BaseServiceWithAudit<PaymentStatus, PaymentOutput> {
    public ProcessPaymentStatusService(PaymentStatusRepository repository, ProcessPaymentStatusCommand command) {
        super(repository, command);
    }
}
