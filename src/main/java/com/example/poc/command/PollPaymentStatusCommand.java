package com.example.poc.command;

import com.example.poc.client.PaymentProvider;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.AckPaymentSentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PollPaymentStatusCommand extends BaseCommand<AckPaymentSent, PaymentStatus> {

    final
    PaymentProvider paymentProviderMock;

    private final AckPaymentSentRepository repository;

    public PollPaymentStatusCommand(@Qualifier("mock") PaymentProvider paymentProviderMock, AckPaymentSentRepository repository) {
        this.paymentProviderMock = paymentProviderMock;
        this.repository = repository;
    }

    @Override
    public PaymentStatus execute(AckPaymentSent detachedAckPaymentSent) {
        super.execute(detachedAckPaymentSent, repository);

        Logger logger = LoggerFactory.getLogger(this.getClass());

        try {
            long time = (long)(Math.random() * 2000); // wait between 0 and 2 seconds
            logger.info("Started polling...(for {}ms)", time);
            // Verify and show the use of virtual threads
            logger.info("Thread: {} isVirtual? {}", Thread.currentThread(), Thread.currentThread().isVirtual());
            Thread.sleep(time);
            logger.info("Finished polling (--> {}ms)", time);
            return paymentProviderMock.getPaymentStatus(detachedAckPaymentSent);
        } catch (JsonProcessingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
