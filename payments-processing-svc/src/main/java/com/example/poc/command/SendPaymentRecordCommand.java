package com.example.poc.command;

import com.example.poc.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.service.PaymentProviderService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SendPaymentRecordCommand implements Command<PaymentRecord, AckPaymentSent> {

    final
    PaymentProviderService paymentProviderServiceMock;

    // Parameterised constructor for testing purposes
    public SendPaymentRecordCommand(PaymentProviderService paymentProviderServiceMock) {
        this.paymentProviderServiceMock = paymentProviderServiceMock;
    }

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        SendPaymentRequest request = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        return paymentProviderServiceMock.sendPayment(request);
    }
}
