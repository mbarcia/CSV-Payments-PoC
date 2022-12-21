package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class UnparseRecordCommand extends BaseCommand<PaymentStatus, PaymentOutput> {
    public PaymentOutput execute(PaymentStatus paymentStatus) {
        super.execute(paymentStatus);

        AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
        PaymentRecord paymentRecord = ackPaymentSent.getRecord();

        return new PaymentOutput(
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                ackPaymentSent.getConversationID(),
                ackPaymentSent.getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()
        );
    }

    @Override
    protected PaymentStatus persist(PaymentStatus processableObj) {
        return csvPaymentsService.persist(processableObj);
    }
}
