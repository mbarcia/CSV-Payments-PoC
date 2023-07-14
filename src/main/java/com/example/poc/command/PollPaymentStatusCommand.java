package com.example.poc.command;

import com.example.poc.client.PaymentProvider;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PollPaymentStatusCommand extends BaseCommand<AckPaymentSent, PaymentStatus> {

    @Autowired
    @Qualifier("mock")
    PaymentProvider paymentProviderMock;
    @Async
    public PaymentStatus execute(AckPaymentSent detachedAckPaymentSent) {
        super.execute(detachedAckPaymentSent);

        Logger logger = LoggerFactory.getLogger(this.getClass());

        try {
            long time = (long)(Math.random() * 10000);
            logger.info("Started polling...({}ms)", time);
            Thread.sleep(time);
            logger.info("Polled for {}ms", time);
            return paymentProviderMock.getPaymentStatus(detachedAckPaymentSent);
        } catch (JsonProcessingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AckPaymentSent persist(AckPaymentSent processableObj) {
        return csvPaymentsService.persist(processableObj);
    }
}
