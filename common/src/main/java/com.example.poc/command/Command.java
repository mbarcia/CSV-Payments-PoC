package com.example.poc.command;

/**
 * @param <T> Input type for the execute method
 * @param <S> Output type for the execute method
 */
@FunctionalInterface
public interface Command<T, S> {
    S execute(T processableObj);
}

/*
CsvFolder         --> CsvPaymentsInputFile               --> PaymentRecord            --> AckPaymentSent               --> PaymentStatus               --> PaymentOutput

ReadFolderCommand --> ProcessCsvPaymentsInputFileCommand --> SendPaymentRecordCommand --> ProcessAckPaymentSentCommand --> ProcessPaymentStatusCommand --> ProcessPaymentOutputCommand
                                                                                           PollAckPaymentSentCommand --^

ReadFolderService --> ProcessCsvPaymentsInputFileService --> SendPaymentRecordService --> ProcessAckPaymentSentService -->  ProcessPaymentStatusService --> ProcessPaymentOutputService
                                                                                           PollAckPaymentSentService --^
 */