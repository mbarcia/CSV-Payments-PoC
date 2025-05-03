package com.example.poc.mappers;

import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper {
    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "reference", target = "reference")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "uuidToString")
    PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus entity);

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUuid")
    @Mapping(source = "reference", target = "reference")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "stringToUuid")
    PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc);

    @Named("bigDecimalToString")
    static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }

    @Named("stringToBigDecimal")
    static BigDecimal stringToBigDecimal(String value) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Named("uuidToString")
    static String uuidToString(UUID value) {
        return value != null ? value.toString() : null;
    }

    @Named("stringToUuid")
    static UUID stringToUuid(String value) {
        return value != null ? UUID.fromString(value) : null;
    }
}
