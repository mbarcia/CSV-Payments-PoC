package com.example.poc.command;

import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessPaymentStatusCommand implements Command<PaymentStatus, PaymentOutput> {
    @Override
    public PaymentOutput execute(PaymentStatus paymentStatus) {
        PaymentRecord paymentRecord = paymentStatus.getAckPaymentSent().getRecord();

        return new PaymentOutput(
                paymentRecord,
                paymentRecord.getCsvPaymentsOutputFile(),
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                paymentStatus.getAckPaymentSent().getConversationID(),
                paymentStatus.getAckPaymentSent().getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()
        );
    }
}
