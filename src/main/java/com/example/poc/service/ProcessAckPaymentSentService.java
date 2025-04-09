package com.example.poc.service;

import com.example.poc.command.ProcessAckPaymentSentCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.AckPaymentSentRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessAckPaymentSentService extends BaseServiceWithAudit<AckPaymentSent, PaymentStatus> {
    public ProcessAckPaymentSentService(AckPaymentSentRepository repository, ProcessAckPaymentSentCommand command) {
        super(repository, command);
    }
}
