package com.example.poc.client;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Component(value="mock")
public class PaymentProviderMock implements PaymentProvider {

    public static final String UUID = "ac007cbd-1504-4207-8d9f-0abc4b1d2bd8";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public AckPaymentSent sendPayment(SendPaymentRequest requestMap) {

        requestMap.getUrl();
        requestMap.getAmount();
        requestMap.getMsisdn();
        requestMap.getRecord();
        requestMap.getCurrency();
        requestMap.getReference();
        return new AckPaymentSent(UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(requestMap.getRecord());

    }

    @Override
    public PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) {

        return new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(ackPaymentSent);
    }
}
