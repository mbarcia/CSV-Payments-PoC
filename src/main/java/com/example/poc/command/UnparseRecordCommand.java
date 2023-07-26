package com.example.poc.command;

import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class UnparseRecordCommand extends BaseCommand<PaymentStatus, PaymentOutput> {
    @Override
    public PaymentOutput execute(PaymentStatus paymentStatus) {
        super.execute(paymentStatus);

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

    @Override
    protected PaymentStatus persist(PaymentStatus processableObj) {
        return csvPaymentsService.persist(processableObj);
    }
}
