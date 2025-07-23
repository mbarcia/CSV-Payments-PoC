package com.example.poc.command;

import com.example.poc.common.command.ReactiveCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.service.PaymentProviderConfig;
import com.example.poc.service.PaymentProviderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PollAckPaymentSentCommand implements ReactiveCommand<AckPaymentSent, PaymentStatus> {

  private Executor executor;

  @Inject
  public PollAckPaymentSentCommand(@Named("virtualExecutor") Executor executor) {
    this.executor = executor;
  }

  @Inject PaymentProviderService paymentProviderServiceMock;

  @Inject PaymentProviderConfig config;

  // Parameterised constructor for testing purposes
  public PollAckPaymentSentCommand(
      Executor executor,
      PaymentProviderService paymentProviderServiceMock,
      PaymentProviderConfig config) {
    this.executor = executor;
    this.paymentProviderServiceMock = paymentProviderServiceMock;
    this.config = config;
  }

  /**
   * @param detachedAckPaymentSent Input to poll the payment provider service
   * @return Output from the payment provider service
   */
  @Override
  public Uni<PaymentStatus> execute(AckPaymentSent detachedAckPaymentSent) {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    return Uni.createFrom()
        .item(detachedAckPaymentSent)
        .runSubscriptionOn(executor)
        .map(
            ack -> {
              try {
                long time = (long) (Math.random() * config.waitMilliseconds());
                logger.info("Started polling...(for {}ms)", time);
                logger.info(
                    "Thread: {} isVirtual? {}",
                    Thread.currentThread(),
                    Thread.currentThread().isVirtual());
                Thread.sleep(time); // simulate delay
                logger.info("Finished polling (--> {}ms)", time);
                return paymentProviderServiceMock.getPaymentStatus(ack);
              } catch (JsonProcessingException | InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
  }

  // for testing purposes
  void setExecutor(Executor executor) {
    this.executor = executor;
  }
}
