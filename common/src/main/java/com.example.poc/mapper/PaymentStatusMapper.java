package com.example.poc.mapper;

import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class, AckPaymentSentMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper {
    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "reference", target = "reference")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "uuidToString")
    @Mapping(source = "ackPaymentSent", target = "ackPaymentSent")
    PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus entity);

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUUID")
    @Mapping(source = "reference", target = "reference")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "ackPaymentSentId", target = "ackPaymentSentId", qualifiedByName = "stringToUUID")
    @Mapping(source = "ackPaymentSent", target = "ackPaymentSent")
    PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc);
}
