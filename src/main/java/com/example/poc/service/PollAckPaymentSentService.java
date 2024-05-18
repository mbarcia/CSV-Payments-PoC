package com.example.poc.service;

import com.example.poc.command.PollAckPaymentSentCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.AckPaymentSentRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class PollAckPaymentSentService extends BaseServiceWithAudit<AckPaymentSent, PaymentStatus> {
    public PollAckPaymentSentService(AckPaymentSentRepository repository, PollAckPaymentSentCommand command) {
        super(repository, command);
    }
}
