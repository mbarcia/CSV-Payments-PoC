package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessAckPaymentSentCommand implements Command<AckPaymentSent, PaymentStatus> {
    private final PollAckPaymentSentService pollAckPaymentSentService;

    private final ExecutorService executorService;

    public ProcessAckPaymentSentCommand(PollAckPaymentSentService pollAckPaymentSentService) {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.pollAckPaymentSentService = pollAckPaymentSentService;
    }

    @Override
    public PaymentStatus execute(AckPaymentSent ackPaymentSent) {
        // call the polling/blocking service on a virtual thread (async)
        CompletableFuture<PaymentStatus> paymentStatusCompletableFuture = CompletableFuture.supplyAsync(() -> pollAckPaymentSentService.process(ackPaymentSent), executorService);
        // dump the transformed payment data into an output record
        try {
            return paymentStatusCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
