package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.CsvPaymentsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PollPaymentStatusCommand extends BaseCommand<AckPaymentSent, PaymentStatus> {
    @Autowired
    PaymentProviderClient client;

    @Autowired
    CsvPaymentsServiceImpl csvPaymentsService;

    @Async
    public PaymentStatus execute(AckPaymentSent detachedAckPaymentSent) {
        super.execute(detachedAckPaymentSent);
        PaymentStatus paymentStatus;

//        TODO
//        try {
//            paymentStatus = (client.getPaymentStatus(detachedAckPaymentSent)).setAckPaymentSent(detachedAckPaymentSent);
        paymentStatus = new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(detachedAckPaymentSent);
        csvPaymentsService.persistPaymentStatus(paymentStatus);
//        } catch (JsonProcessingException e) {
//            Logger logger = LoggerFactory.getLogger(this.getClass());
//            logger.error(e.getLocalizedMessage());
//            return null;
//        }

        return paymentStatus;
    }
}
