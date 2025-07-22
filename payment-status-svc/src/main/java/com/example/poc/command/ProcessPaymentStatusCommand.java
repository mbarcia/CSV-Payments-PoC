package com.example.poc.command;

import com.example.poc.common.command.ReactiveCommand;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessPaymentStatusCommand implements ReactiveCommand<PaymentStatus, PaymentOutput> {
    @Override
    public Uni<PaymentOutput> execute(PaymentStatus paymentStatus) {
        PaymentRecord paymentRecord = paymentStatus.getAckPaymentSent().getPaymentRecord();

        return Uni.createFrom().item(new PaymentOutput(
            paymentStatus,
            paymentRecord.getCsvId(),
            paymentRecord.getRecipient(),
            paymentRecord.getAmount(),
            paymentRecord.getCurrency(),
            paymentStatus.getAckPaymentSent().getConversationId(),
            paymentStatus.getAckPaymentSent().getStatus(),
            paymentStatus.getMessage(),
            paymentStatus.getFee()
        ));
    }
}
