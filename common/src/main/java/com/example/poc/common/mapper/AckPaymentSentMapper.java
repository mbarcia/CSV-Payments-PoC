package com.example.poc.common.mapper;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.dto.AckPaymentSentDto;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class, PaymentRecordMapper.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AckPaymentSentMapper {

    // Domain ↔ DTO
    @Mapping(target = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "paymentRecordId", qualifiedByName = "uuidToString")
    @Mapping(target = "status", qualifiedByName = "longToString")
    @Mapping(target = "message")
    AckPaymentSentDto toDto(AckPaymentSent domain);

    @Mapping(target = "id", qualifiedByName = "stringToUUID")
    @Mapping(target = "paymentRecordId", qualifiedByName = "stringToUUID")
    @Mapping(target = "status", qualifiedByName = "stringToLong")
    @Mapping(target = "message")
    AckPaymentSent fromDto(AckPaymentSentDto dto);

    // DTO ↔ gRPC
    PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSentDto dto);
    AckPaymentSentDto fromGrpcToDto(PaymentsProcessingSvc.AckPaymentSent grpc);

    // Domain ↔ DTO ↔ gRPC
    default PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domain) {
        return toGrpc(toDto(domain));
    }

    default AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent grpc) {
        return fromDto(fromGrpcToDto(grpc));
    }
}
