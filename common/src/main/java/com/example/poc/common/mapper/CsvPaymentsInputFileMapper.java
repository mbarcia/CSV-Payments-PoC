package com.example.poc.common.mapper;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputFileMapper {

    @Mapping(target = "filepath")
    @Mapping(target = "csvFolderPath")
    InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFile entity);
    CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile proto);
}
