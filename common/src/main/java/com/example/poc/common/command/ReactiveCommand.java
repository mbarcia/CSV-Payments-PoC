package com.example.poc.common.command;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface ReactiveCommand<T, S> {
  Uni<S> execute(T input);
}

/*
CsvFolder         --> CsvPaymentsInputFile               --> PaymentRecord            --> AckPaymentSent               --> PaymentStatus               --> PaymentOutput

ReadFolderCommand --> ProcessCsvPaymentsInputFileCommand --> SendPaymentRecordCommand --> ProcessAckPaymentSentCommand --> ProcessPaymentStatusCommand --> ProcessPaymentOutputCommand
                                                                                           PollAckPaymentSentCommand --^

ReadFolderService --> ProcessCsvPaymentsInputFileService --> SendPaymentRecordService --> ProcessAckPaymentSentService -->  ProcessPaymentStatusService --> ProcessPaymentOutputService
                                                                                           PollAckPaymentSentService --^
 */
