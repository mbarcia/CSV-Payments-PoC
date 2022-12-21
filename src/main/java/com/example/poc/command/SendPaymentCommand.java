package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.service.CsvPaymentsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendPaymentCommand extends BaseCommand<PaymentRecord, AckPaymentSent> {

    public static final String DUMMY_MSISDN = "12125551003";
    @Autowired
    PaymentProviderClient client;

    @Autowired
    CsvPaymentsServiceImpl csvPaymentsService;

    @Override
    public AckPaymentSent execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        AckPaymentSent result = new AckPaymentSent(String.valueOf(UUID.randomUUID()))
        .setStatus(1000L)
        .setMessage("OK but this is only a test");

//        TODO
//        AckPaymentSent result = client.sendPayment((new SendPaymentRequest()).
//                setMsisdn(DUMMY_MSISDN).
//                setAmount(paymentRecord.getAmount()).
//                setCurrency(paymentRecord.getCurrency()));

        return csvPaymentsService.persistAckPaymentSent(result.setRecord(paymentRecord));
    }
}
