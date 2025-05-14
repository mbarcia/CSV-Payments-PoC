package com.example.poc.mapper;

import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentRecordMapper {

    // Entity to gRPC
    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(source = "csvPaymentsInputFileId", target = "csvPaymentsInputFileId", qualifiedByName = "uuidToString")
    @Mapping(source = "csvPaymentsOutputFileId", target = "csvPaymentsOutputFileId", qualifiedByName = "uuidToString")
    InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord entity);

    // gRPC to Entity
    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUUID")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "stringToCurrency")
    @Mapping(target = "csvPaymentsInputFile", ignore = true)
    @Mapping(target = "csvPaymentsOutputFile", ignore = true)
    @Mapping(source = "csvPaymentsInputFileId", target = "csvPaymentsInputFileId", qualifiedByName = "stringToUUID")
    @Mapping(source = "csvPaymentsOutputFileId", target = "csvPaymentsOutputFileId", qualifiedByName = "stringToUUID")
    PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpc);
}