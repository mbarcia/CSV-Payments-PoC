package com.example.poc.domain;

import com.example.poc.grpc.PaymentStatusSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper {

    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "toUUID")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "toBigDecimal")
    PaymentStatus fromGrpc(PaymentStatusSvc.PaymentStatus proto);

    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "toString")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "toStringDecimal")
    PaymentStatusSvc.PaymentStatus toGrpc(PaymentStatus domain);

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
}
