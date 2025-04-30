package com.example.poc.domain;

import com.example.poc.grpc.PaymentStatusSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentOutputMapper {

    @Mapping(target = "id", ignore = true) // managed by JPA
    @Mapping(target = "paymentRecord", ignore = true) // transient
    @Mapping(target = "csvPaymentsOutputFile", ignore = true) // transient
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "toUUID")
    @Mapping(source = "csvPaymentsOutputFilename", target = "csvPaymentsOutputFilename")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "toBigDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "toCurrency")
    @Mapping(source = "conversationID", target = "conversationID")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "toBigDecimal")
    PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput proto);

    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "toString")
    @Mapping(source = "csvPaymentsOutputFilename", target = "csvPaymentsOutputFilename")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "toStringDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(source = "conversationID", target = "conversationID")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "toStringDecimal")
    PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domain);

    @Named("toUUID")
    static UUID toUUID(String id) {
        return id == null || id.isBlank() ? null : UUID.fromString(id);
    }

    @Named("toString")
    static String toString(UUID id) {
        return id == null ? "" : id.toString();
    }

    @Named("toBigDecimal")
    static BigDecimal toBigDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    @Named("toStringDecimal")
    static String toStringDecimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    @Named("toCurrency")
    static Currency toCurrency(String value) {
        return value == null || value.isBlank() ? null : Currency.getInstance(value);
    }

    @Named("currencyToString")
    static String currencyToString(Currency value) {
        return value == null ? "" : value.getCurrencyCode();
    }
}
