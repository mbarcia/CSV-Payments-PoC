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

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.service.ReactiveStreamingService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Executor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
@Getter
public class ProcessCsvPaymentsInputFileReactiveService
    implements ReactiveStreamingService<CsvPaymentsInputFile, PaymentRecord> {

  Executor executor;

  @Inject
  public ProcessCsvPaymentsInputFileReactiveService(@Named("virtualExecutor") Executor executor) {
        this.executor = executor;
    }

  @Override
  public Multi<PaymentRecord> process(CsvPaymentsInputFile csvFile) {
    Logger logger = LoggerFactory.getLogger(getClass());
    var strategy = new FilePathAwareMappingStrategy<PaymentRecord>(csvFile.getFilepath());
    strategy.setType(PaymentRecord.class);

    try {
      CsvToBean<PaymentRecord> csvReader =
              new CsvToBeanBuilder<PaymentRecord>(
                      new BufferedReader(new FileReader(String.valueOf(csvFile.getFilepath()))))
                      .withType(PaymentRecord.class)
                      .withMappingStrategy(strategy)
                      .withSeparator(',')
                      .withIgnoreLeadingWhiteSpace(true)
                      .withIgnoreEmptyLine(true)
                      .build();
      String serviceId = this.getClass().toString();

      return Multi.createFrom()
              .items(() -> csvReader.parse().stream())
              .runSubscriptionOn(executor)
              .invoke(result -> {
                MDC.put("serviceId", serviceId);
                logger.info("Executed command on {} --> {}", csvFile, result);
                MDC.clear();
              });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      throw new RuntimeException(e);
    }
  }
}
