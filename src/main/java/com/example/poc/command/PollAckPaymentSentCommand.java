package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PaymentProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PollAckPaymentSentCommand implements Command<AckPaymentSent, PaymentStatus> {

    final
    PaymentProvider paymentProviderMock;

    public PollAckPaymentSentCommand(PaymentProvider paymentProviderMock) {
        this.paymentProviderMock = paymentProviderMock;
    }

    /**
     * @param detachedAckPaymentSent Input to poll the payment provider service
     * @return Output from the payment provider service
     */
    @Override
    public PaymentStatus execute(AckPaymentSent detachedAckPaymentSent) {
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
