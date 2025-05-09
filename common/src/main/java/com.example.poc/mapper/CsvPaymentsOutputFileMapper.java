package com.example.poc.mapper;

import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsOutputFileMapper {

    @Mapping(target = "csvFile", ignore = true)
    @Mapping(source = "filepath", target = "filepath")
    @Mapping(target = "csvFolder", ignore = true) // handled elsewhere
    @Mapping(target = "csvFolderId", ignore = true) // handled elsewhere
    @Mapping(target = "sbc", ignore = true) // handled elsewhere
    @Mapping(target = "writer", ignore = true) // handled elsewhere
    CsvPaymentsOutputFile fromGrpc(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile proto);

    @Mapping(target = "filepath", source = "filepath")
    OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile entity);
}
