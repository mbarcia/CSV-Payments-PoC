package com.example.poc.command;

import com.example.poc.Command;
import com.example.poc.biz.PaymentRecord;

public class SendPaymentCommand implements Command<PaymentRecord, PaymentRecord> {
    @Override
    public PaymentRecord execute(PaymentRecord paymentRecord) {
        // call the API

        // what if the call failed?
//        paymentRecord.setStatus(FAILED);

        // return same object so the stream processing can continue
        return paymentRecord;
    }
}
