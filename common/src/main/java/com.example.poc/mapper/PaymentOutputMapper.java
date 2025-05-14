package com.example.poc.mapper;

import com.example.poc.domain.PaymentOutput;
import com.example.poc.grpc.PaymentStatusSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentOutputMapper {

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUUID")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    @Mapping(target = "csvPaymentsOutputFile", ignore = true) // transient
    @Mapping(source = "csvPaymentsOutputFilename", target = "csvPaymentsOutputFilename")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "stringToCurrency")
    @Mapping(source = "conversationId", target = "conversationId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "stringToBigDecimal")
    PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput proto);

    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    @Mapping(source = "csvPaymentsOutputFilename", target = "csvPaymentsOutputFilename")
    @Mapping(source = "csvId", target = "csvId")
    @Mapping(source = "recipient", target = "recipient")
    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(source = "conversationId", target = "conversationId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "bigDecimalToString")
    PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domain);
}
