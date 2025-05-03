package com.example.poc.mappers;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputFileMapper {

    InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFile entity);

    CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile message);
}
