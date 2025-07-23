package com.example.poc.service;

import com.example.poc.command.ProcessPaymentOutputCommand;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.service.BaseReactiveStreamingClientService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class ProcessCsvPaymentsOutputFileReactiveService
    extends BaseReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {
  @Inject ProcessPaymentOutputCommand command;

  @Override
  public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> processableObj) {
    return super.process(processableObj);
  }
}
