package com.example.poc.command;

import com.example.poc.common.command.ReactiveStreamingClientCommand;
import com.example.poc.common.domain.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.concurrent.Executor;

@ApplicationScoped
public class ProcessPaymentOutputCommand
    implements ReactiveStreamingClientCommand<PaymentOutput, CsvPaymentsOutputFile> {

  private Executor executor;

  @Inject
  public ProcessPaymentOutputCommand(@Named("virtualExecutor") Executor executor) {
    this.executor = executor;
  }

  public Uni<CsvPaymentsOutputFile> execute(Multi<PaymentOutput> paymentOutputList) {
    return paymentOutputList
        .collect()
        .asList()
        .onItem()
        .transformToUni(
            paymentOutputs ->
                Uni.createFrom()
                    .item(
                        () -> {
                          try (CsvPaymentsOutputFile file =
                              this.getCsvPaymentsOutputFile(paymentOutputs.getFirst())) {
                            file.getSbc().write(paymentOutputs);
                            return file;
                          } catch (Exception e) {
                            throw new RuntimeException("Failed to write output file.", e);
                          }
                        })
                    .runSubscriptionOn(executor));
  }

  protected CsvPaymentsOutputFile getCsvPaymentsOutputFile(PaymentOutput paymentOutput)
      throws IOException {
    assert paymentOutput != null;
    PaymentStatus paymentStatus = paymentOutput.getPaymentStatus();
    AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
    PaymentRecord paymentRecord = ackPaymentSent.getPaymentRecord();
    String csvPaymentsInputFilePath = paymentRecord.getCsvPaymentsInputFilePath();

    return new CsvPaymentsOutputFile(csvPaymentsInputFilePath);
  }

  // for test use
  void setExecutor(Executor executor) {
    this.executor = executor;
  }
}
