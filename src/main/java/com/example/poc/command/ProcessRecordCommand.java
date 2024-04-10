package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PollPaymentStatusService;
import com.example.poc.service.SendPaymentService;
import com.example.poc.service.UnparseRecordService;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessRecordCommand implements Command<PaymentRecord, PaymentOutput> {
    private final SendPaymentService sendPaymentService;
    private final PollPaymentStatusService pollPaymentStatusService;
    private final UnparseRecordService unparseRecordService;

    private final ExecutorService executorService;

    public ProcessRecordCommand(SendPaymentService sendPaymentService, PollPaymentStatusService pollPaymentStatusService, UnparseRecordService unparseRecordService) {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.sendPaymentService = sendPaymentService;
        this.pollPaymentStatusService = pollPaymentStatusService;
        this.unparseRecordService = unparseRecordService;
    }

    @Override
    public PaymentOutput execute(PaymentRecord paymentRecord) {
        // post the payment record to the API service
        AckPaymentSent ackPaymentSent = sendPaymentService.process(paymentRecord);
        // call the polling/blocking service on a virtual thread (async)
        CompletableFuture<PaymentStatus> paymentStatusCompletableFuture = CompletableFuture.supplyAsync(() -> pollPaymentStatusService.process(ackPaymentSent), executorService);
        // dump the transformed payment data into an output record
        PaymentOutput paymentOutput;
        try {
            paymentOutput = paymentStatusCompletableFuture.thenApply(unparseRecordService::process).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        // write the output record to the CSV file
        try {
            paymentRecord.getCsvPaymentsFile().getSbc().write(paymentOutput);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RuntimeException(e);
        }

        return paymentOutput;
    }
}
