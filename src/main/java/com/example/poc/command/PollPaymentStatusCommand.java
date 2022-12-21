package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PollPaymentStatusCommand extends BaseCommand<AckPaymentSent, PaymentStatus> {

    @Async
    public PaymentStatus execute(AckPaymentSent detachedAckPaymentSent) {
        super.execute(detachedAckPaymentSent);

/*
        This code should look like this if there was a client available:

        try {
            return (client.getPaymentStatus(detachedAckPaymentSent)).setAckPaymentSent(detachedAckPaymentSent);
        } catch (JsonProcessingException e) {
            Logger logger = LoggerFactory.getLogger(this.getClass());
            logger.error(e.getLocalizedMessage());
            return null;
        }
      Returning a dummy object instead
*/

        return new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(detachedAckPaymentSent);
    }

    @Override
    protected AckPaymentSent persist(AckPaymentSent processableObj) {
        return csvPaymentsService.persist(processableObj);
    }
}
