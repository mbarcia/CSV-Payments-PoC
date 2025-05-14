package com.example.poc.mapper;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class, PaymentRecord.class, PaymentStatusMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AckPaymentSentMapper {

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUUID")
    @Mapping(source = "conversationId", target = "conversationId")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "stringToUUID")
    @Mapping(source = "paymentRecord", target = "paymentRecord")
    AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent proto);

    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "conversationId", target = "conversationId")
    @Mapping(source = "paymentRecordId", target = "paymentRecordId", qualifiedByName = "uuidToString")
    @Mapping(source = "paymentRecord", target = "paymentRecord")
    PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domain);
}
