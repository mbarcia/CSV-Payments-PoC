package com.example.poc.service;

import com.example.poc.command.PollPaymentStatusCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class PollPaymentStatusService extends BaseService<AckPaymentSent, PaymentStatus> {
    public PollPaymentStatusService(CrudRepository<AckPaymentSent, Long> repository, PollPaymentStatusCommand command) {
        super(repository, command);
    }
}
