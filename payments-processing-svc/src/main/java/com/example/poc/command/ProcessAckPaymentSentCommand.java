package com.example.poc.command;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProcessAckPaymentSentCommand implements Command<AckPaymentSent, PaymentStatus> {
    @Inject
    PollAckPaymentSentService pollAckPaymentSentService;

    public ProcessAckPaymentSentCommand(PollAckPaymentSentService pollAckPaymentSentService) {
        this.pollAckPaymentSentService = pollAckPaymentSentService;
    }

    @Override
    public PaymentStatus execute(AckPaymentSent ackPaymentSent) {
        // Directly call the service without threading
        return pollAckPaymentSentService.process(ackPaymentSent);
    }
}
