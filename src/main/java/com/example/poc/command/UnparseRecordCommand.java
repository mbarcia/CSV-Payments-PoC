package com.example.poc.command;

import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import org.springframework.stereotype.Component;

@Component
public class UnparseRecordCommand extends BaseCommand<PaymentStatus, PaymentOutput> {
    private final PaymentStatusRepository repository;

    public UnparseRecordCommand(PaymentStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentOutput execute(PaymentStatus paymentStatus) {
        super.execute(paymentStatus, repository);

        PaymentRecord paymentRecord = paymentStatus.getAckPaymentSent().getRecord();

        return new PaymentOutput(
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
