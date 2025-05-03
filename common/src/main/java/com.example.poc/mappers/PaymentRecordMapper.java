package com.example.poc.mappers;

import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Mapper(componentModel = "cdi")
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
    @Mapping(source = "csvPaymentsInputFileId", target = "csvPaymentsInputFileId", qualifiedByName = "stringToUUID")
    @Mapping(source = "csvPaymentsOutputFileId", target = "csvPaymentsOutputFileId", qualifiedByName = "stringToUUID")
    PaymentRecord toEntity(InputCsvFileProcessingSvc.PaymentRecord grpc);

    // Custom mappers
    @Named("uuidToString")
    static String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : "";
    }

    @Named("stringToUUID")
    static UUID stringToUUID(String str) {
        return (str != null && !str.isEmpty()) ? UUID.fromString(str) : null;
    }

    @Named("bigDecimalToString")
    static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }

    @Named("stringToBigDecimal")
    static BigDecimal stringToBigDecimal(String str) {
        return (str != null && !str.isEmpty()) ? new BigDecimal(str) : BigDecimal.ZERO;
    }

    @Named("currencyToString")
    static String currencyToString(Currency currency) {
        return currency != null ? currency.getCurrencyCode() : "";
    }

    @Named("stringToCurrency")
    static Currency stringToCurrency(String str) {
        return (str != null && !str.isEmpty()) ? Currency.getInstance(str) : null;
    }
}