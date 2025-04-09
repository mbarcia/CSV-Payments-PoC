package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessAckPaymentSentCommand implements Command<AckPaymentSent, PaymentStatus> {
    private final PollAckPaymentSentService pollAckPaymentSentService;

    public ProcessAckPaymentSentCommand(PollAckPaymentSentService pollAckPaymentSentService) {
        this.pollAckPaymentSentService = pollAckPaymentSentService;
    }

    @Override
    public PaymentStatus execute(AckPaymentSent ackPaymentSent) {
        // Directly call the service without threading
        return pollAckPaymentSentService.process(ackPaymentSent);
    }
}
