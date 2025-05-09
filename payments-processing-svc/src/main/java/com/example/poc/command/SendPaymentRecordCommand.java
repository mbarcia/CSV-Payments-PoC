package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.mapper.SendPaymentRequestMapper;
import com.example.poc.service.PaymentProviderServiceMock;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SendPaymentRecordCommand implements Command<PaymentRecord, AckPaymentSent> {

    final
    PaymentProviderServiceMock paymentProviderServiceMock;

    // Parameterised constructor for testing purposes
    public SendPaymentRecordCommand(PaymentProviderServiceMock paymentProviderServiceMock) {
        this.paymentProviderServiceMock = paymentProviderServiceMock;
    }

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        SendPaymentRequestMapper.SendPaymentRequest request = new SendPaymentRequestMapper.SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setPaymentRecord(paymentRecord)
                .setPaymentRecordId(paymentRecord.getId());

        return paymentProviderServiceMock.sendPayment(request);
    }
}
