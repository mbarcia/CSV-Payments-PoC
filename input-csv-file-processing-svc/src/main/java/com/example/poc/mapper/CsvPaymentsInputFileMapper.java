package com.example.poc.mapper;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.io.File;
import java.util.UUID;

@Mapper(componentModel = "cdi", imports = {File.class, UUID.class})
public interface CsvPaymentsInputFileMapper {

    @Mapping(target = "csvFile", ignore = true)
    @Mapping(source = "filepath", target = "filepath")
    @Mapping(target = "csvFolder", ignore = true) // handled elsewhere
    @Mapping(target = "csvFolderId", ignore = true) // handled elsewhere
    CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile proto);

    @Mapping(target = "filepath", source = "filepath")
    InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFile entity);
}
