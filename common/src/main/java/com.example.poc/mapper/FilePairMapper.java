package com.example.poc.mapper;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface FilePairMapper {

    default OutputCsvFileProcessingSvc.InitialiseFilesRequest toProtoList(Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> map) {
        List<OutputCsvFileProcessingSvc.FilePair> protoPairs = map.entrySet().stream()
                .map(entry -> OutputCsvFileProcessingSvc.FilePair.newBuilder()
                        .setInput(toProtoInput(entry.getKey()))
                        .setOutput(toProtoOutput(entry.getValue()))
                        .build())
                .collect(Collectors.toList());

        return OutputCsvFileProcessingSvc.InitialiseFilesRequest.newBuilder()
                .addAllFiles(protoPairs)
                .build();
    }

    InputCsvFileProcessingSvc.CsvPaymentsInputFile toProtoInput(CsvPaymentsInputFile input);
    OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toProtoOutput(CsvPaymentsOutputFile output);

    // From proto to POJO
    CsvPaymentsInputFile fromProtoInput(InputCsvFileProcessingSvc.CsvPaymentsInputFile proto);
    CsvPaymentsOutputFile fromProtoOutput(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile proto);

    default Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> fromProtoList(OutputCsvFileProcessingSvc.InitialiseFilesRequest request) {
        return request.getFilesList().stream()
                .collect(Collectors.toMap(
                        pair -> fromProtoInput(pair.getInput()),
                        pair -> fromProtoOutput(pair.getOutput())
                ));
    }
}
