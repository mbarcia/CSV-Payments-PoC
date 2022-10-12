package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.SendPayment;
import com.example.poc.repository.SendPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, PaymentRecord> {

    @Autowired
    PaymentProviderClient client;

    @Autowired
    SendPaymentRepository sendPaymentRepository;

    @Transactional
    @Override
    public PaymentRecord execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        SendPaymentRequest request = new SendPaymentRequest();
        request.setMsisdn("12125551003");
        request.setAmount(paymentRecord.getAmount());
        request.setCurrency(paymentRecord.getCurrency());

        SendPayment result = client.pay(request);
        result.setRecord(paymentRecord);
        sendPaymentRepository.save(result);

        return paymentRecord;
    }
}
