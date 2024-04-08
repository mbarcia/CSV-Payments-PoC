package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentRecordRepository;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessRecordCommand extends BaseCommand<PaymentRecord, PaymentOutput> {
    private final SendPaymentCommand sendPaymentCommand;
    private final PollPaymentStatusCommand pollPaymentStatusCommand;
    private final UnparseRecordCommand unparseRecordCommand;
    private final PaymentRecordRepository repository;

    private final ExecutorService executorService;

    public ProcessRecordCommand(SendPaymentCommand sendPaymentCommand, PollPaymentStatusCommand pollPaymentStatusCommand, UnparseRecordCommand unparseRecordCommand, PaymentRecordRepository repository) {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.sendPaymentCommand = sendPaymentCommand;
        this.pollPaymentStatusCommand = pollPaymentStatusCommand;
        this.unparseRecordCommand = unparseRecordCommand;
        this.repository = repository;
    }

    @Override
    public PaymentOutput execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord, repository);

        // post the payment record to the API service
        AckPaymentSent ackPaymentSent = sendPaymentCommand.execute(paymentRecord);
        // call the polling/blocking service on a virtual thread (async)
        CompletableFuture<PaymentStatus> paymentStatusCompletableFuture = CompletableFuture.supplyAsync(() -> pollPaymentStatusCommand.execute(ackPaymentSent), executorService);
        // dump the transformed payment data into an output record
        PaymentOutput paymentOutput;
        try {
            paymentOutput = paymentStatusCompletableFuture.thenApply(unparseRecordCommand::execute).get();
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
