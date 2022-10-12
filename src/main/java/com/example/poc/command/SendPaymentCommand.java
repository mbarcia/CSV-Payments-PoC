package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.repository.AckPaymentSentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, AckPaymentSent> {

    @Autowired
    PaymentProviderClient client;

    @Autowired
    AckPaymentSentRepository ackPaymentSentRepository;

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        SendPaymentRequest request = new SendPaymentRequest();
        request.setMsisdn("12125551003");
        request.setAmount(paymentRecord.getAmount());
        request.setCurrency(paymentRecord.getCurrency());

//        TODO
        AckPaymentSent result = client.sendPayment(request);
//        AckPaymentSent result = new AckPaymentSent();
//        result.setStatus(1000L);
//        result.setMessage("this is a test");
//        result.setConversationID("1234567890");

        result.setRecord(paymentRecord);
        ackPaymentSentRepository.save(result);

        return result;
    }
}
