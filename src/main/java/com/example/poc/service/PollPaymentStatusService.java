package com.example.poc.service;

import com.example.poc.command.PollPaymentStatusCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.AckPaymentSentRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class PollPaymentStatusService extends BaseService<AckPaymentSent, PaymentStatus> {
    public PollPaymentStatusService(AckPaymentSentRepository repository, PollPaymentStatusCommand command) {
        super(repository, command);
    }
}
