package com.example.poc.service;

import com.example.poc.command.ProcessCsvPaymentsInputFileCommand;
import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.service.BaseReactiveStreamingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class ProcessCsvPaymentsInputFileReactiveService
    extends BaseReactiveStreamingService<CsvPaymentsInputFile, PaymentRecord> {

  @Inject ProcessCsvPaymentsInputFileCommand command;
}
