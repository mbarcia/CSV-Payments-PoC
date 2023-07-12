package com.example.poc.command;

import com.example.poc.client.PaymentProvider;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
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

        try {
            return paymentProviderMock.getPaymentStatus(detachedAckPaymentSent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AckPaymentSent persist(AckPaymentSent processableObj) {
        return csvPaymentsService.persist(processableObj);
    }
}
