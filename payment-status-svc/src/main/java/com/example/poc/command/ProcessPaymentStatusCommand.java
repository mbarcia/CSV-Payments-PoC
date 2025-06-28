package com.example.poc.command;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessPaymentStatusCommand implements Command<PaymentStatus, PaymentOutput> {
    @Override
    public PaymentOutput execute(PaymentStatus paymentStatus) {
        PaymentRecord paymentRecord = paymentStatus.getAckPaymentSent().getPaymentRecord();

        return new PaymentOutput(
                paymentStatus,
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                paymentStatus.getAckPaymentSent().getConversationId(),
                paymentStatus.getAckPaymentSent().getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()
        );
    }
}
