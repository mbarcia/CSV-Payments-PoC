package com.example.poc.service;

import com.example.poc.command.PollAckPaymentSentCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.LocalAbstractServiceWithAudit;
import com.example.poc.repository.AckPaymentSentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

@ApplicationScoped
@Getter
public class PollAckPaymentSentService extends LocalAbstractServiceWithAudit<AckPaymentSent, PaymentStatus> {
    public PollAckPaymentSentService(AckPaymentSentRepository repository, PollAckPaymentSentCommand command) {
        super(repository, command);
    }

    protected void persist(AckPaymentSent processableObj) {
        // No persistence for this service
    }
}
