package com.example.poc.domain;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AckPaymentSentMapper {

    @Mapping(source = "id", target = "id", qualifiedByName = "toUUID")
    @Mapping(source = "recordId", target = "recordId", qualifiedByName = "toUUID")
    @Mapping(source = "paymentStatusId", target = "paymentStatusId", qualifiedByName = "toUUID")
    AckPaymentSent fromGrpc(com.example.poc.grpc.PaymentStatusSvc.AckPaymentSent proto);

    @Mapping(source = "id", target = "id", qualifiedByName = "toString")
    @Mapping(source = "recordId", target = "recordId", qualifiedByName = "toString")
    @Mapping(source = "paymentStatusId", target = "paymentStatusId", qualifiedByName = "toString")
    com.example.poc.grpc.PaymentStatusSvc.AckPaymentSent toGrpc(AckPaymentSent domain);

    @Named("toUUID")
    static UUID toUUID(String id) {
        return id == null || id.isBlank() ? null : UUID.fromString(id);
    }

    @Named("toString")
    static String toString(UUID id) {
        return id == null ? "" : id.toString();
    }
}
