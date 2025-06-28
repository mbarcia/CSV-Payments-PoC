package com.example.poc.common.mapper;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsOutputFileMapper {

    @Mapping(target = "filepath")
    @Mapping(target = "csvFolderPath") // handled elsewhere
    OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile entity);
    CsvPaymentsOutputFile fromGrpc(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile proto);
}
