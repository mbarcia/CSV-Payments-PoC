package com.example.poc.common.mapper;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsOutputFileMapper {

  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "filepath")
  @Mapping(target = "csvFolderPath")
  OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile entity);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "filepath")
  @Mapping(target = "csvFolderPath")
  @Mapping(target = "csvFile", ignore = true)
  @Mapping(target = "csvFolder", ignore = true)
  @Mapping(target = "writer", ignore = true)
  @Mapping(target = "sbc", ignore = true)
  @Mapping(target = "paymentOutputs", ignore = true)
  CsvPaymentsOutputFile fromGrpc(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile proto);
}
