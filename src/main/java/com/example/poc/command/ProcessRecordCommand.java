package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentRecordRepository;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessRecordCommand extends BaseCommand<PaymentRecord, PaymentOutput> {
    @Autowired
    private SendPaymentCommand sendPaymentCommand;
    @Autowired
    private PollPaymentStatusCommand pollPaymentStatusCommand;
    @Autowired
    private UnparseRecordCommand unparseRecordCommand;
    @Autowired
    private PaymentRecordRepository repository;

    private final ExecutorService executorService;

    public ProcessRecordCommand() {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public PaymentOutput execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord, repository);

        // post the payment record to the API service
        AckPaymentSent ackPaymentSent = sendPaymentCommand.execute(paymentRecord);
        // call the polling/blocking service on a virtual thread (async)
        CompletableFuture<PaymentStatus> paymentStatusCompletableFuture = CompletableFuture.supplyAsync(() -> pollPaymentStatusCommand.execute(ackPaymentSent), executorService);
        // dump the transformed payment data into an output record
        PaymentOutput paymentOutput = null;
        try {
            paymentOutput = paymentStatusCompletableFuture.thenApply(s -> unparseRecordCommand.execute(s)).get();
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
