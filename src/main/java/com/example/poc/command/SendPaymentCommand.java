package com.example.poc.command;

import com.example.poc.biz.PaymentRecord;
import org.springframework.stereotype.Component;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, PaymentRecord> {
    @Override
    public PaymentRecord execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);
        // call the API

        // what if the call failed?
//        paymentRecord.setStatus(FAILED);

        // return same object so the stream processing can continue
        return paymentRecord;
    }
}
