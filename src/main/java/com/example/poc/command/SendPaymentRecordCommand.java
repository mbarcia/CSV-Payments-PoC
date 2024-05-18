package com.example.poc.command;

import com.example.poc.service.PaymentProvider;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendPaymentRecordCommand implements Command<PaymentRecord, AckPaymentSent> {

    final
    PaymentProvider paymentProviderMock;

    public SendPaymentRecordCommand(@Qualifier("mock") PaymentProvider paymentProviderMock) {
        this.paymentProviderMock = paymentProviderMock;
    }

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        SendPaymentRequest request = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        return paymentProviderMock.sendPayment(request);
    }
}
