/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.poc.service;

import com.example.poc.common.domain.*;
import com.example.poc.common.service.ReactiveStreamingClientService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.concurrent.Executor;

@ApplicationScoped
public class ProcessCsvPaymentsOutputFileReactiveService
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {

  @Inject
  @Named("virtualExecutor")
  Executor executor;

  // for testing purposes only
  public ProcessCsvPaymentsOutputFileReactiveService(Executor executor) {
    this.executor = executor;
  }

  @Override
  public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputList) {
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
}
