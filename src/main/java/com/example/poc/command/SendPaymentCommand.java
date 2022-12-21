package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, AckPaymentSent> {

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        AckPaymentSent result = new AckPaymentSent(String.valueOf(UUID.randomUUID()))
                .setStatus(1000L)
                .setMessage("OK but this is only a test");

/*
        This code should look like this, if there was a client available:
        AckPaymentSent result = client.sendPayment((new SendPaymentRequest()).
                setMsisdn(DUMMY_MSISDN).
                setAmount(paymentRecord.getAmount()).
                setCurrency(paymentRecord.getCurrency()));
*/

        return result.setRecord(paymentRecord);
    }

    @Override
    public PaymentRecord persist(PaymentRecord paymentRecord) {
        return csvPaymentsService.persist(paymentRecord);
    }
}
