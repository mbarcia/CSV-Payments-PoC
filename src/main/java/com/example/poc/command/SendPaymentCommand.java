package com.example.poc.command;

import com.example.poc.client.PaymentProvider;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, AckPaymentSent> {

    final
    PaymentProvider paymentProviderMock;

    private final PaymentRecordRepository repository;

    public SendPaymentCommand(@Qualifier("mock") PaymentProvider paymentProviderMock, PaymentRecordRepository repository) {
        this.paymentProviderMock = paymentProviderMock;
        this.repository = repository;
    }

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord, repository);

        SendPaymentRequest request = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        return paymentProviderMock.sendPayment(request);
    }
}
