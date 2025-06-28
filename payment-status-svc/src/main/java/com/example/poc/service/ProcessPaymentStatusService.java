package com.example.poc.service;

import com.example.poc.command.ProcessPaymentStatusCommand;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.LocalAbstractServiceWithAudit;
import com.example.poc.repository.PaymentStatusRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessPaymentStatusService extends LocalAbstractServiceWithAudit<PaymentStatus, PaymentOutput> {
    public ProcessPaymentStatusService(PaymentStatusRepository repository, ProcessPaymentStatusCommand command) {
        super(repository, command);
    }
}
