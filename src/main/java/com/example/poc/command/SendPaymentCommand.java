package com.example.poc.command;

import com.example.poc.client.PaymentProvider;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, AckPaymentSent> {

    @Autowired
    @Qualifier("mock")
    PaymentProvider paymentProviderMock;
    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        SendPaymentRequest request = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        return paymentProviderMock.sendPayment(request);
    }

    @Override
    public PaymentRecord persist(PaymentRecord paymentRecord) {
        return csvPaymentsService.persist(paymentRecord);
    }
}
