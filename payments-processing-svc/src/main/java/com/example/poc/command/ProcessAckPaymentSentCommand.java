package com.example.poc.command;

import com.example.poc.common.command.ReactiveCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProcessAckPaymentSentCommand
    implements ReactiveCommand<AckPaymentSent, PaymentStatus> {
  @Inject PollAckPaymentSentReactiveService pollAckPaymentSentService;

  // Parameterised constructor for testing purposes
  public ProcessAckPaymentSentCommand(PollAckPaymentSentReactiveService pollAckPaymentSentService) {
    this.pollAckPaymentSentService = pollAckPaymentSentService;
  }

  @Override
  public Uni<PaymentStatus> execute(AckPaymentSent ackPaymentSent) {
    // Directly call the service without threading
    return pollAckPaymentSentService.process(ackPaymentSent);
  }
}
