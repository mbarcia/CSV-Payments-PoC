package com.example.poc.mapper;

import com.example.poc.domain.PaymentStatus;
import com.example.poc.dto.PaymentStatusDto;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class, AckPaymentSentMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentStatusMapper {

    PaymentStatusDto toDto(PaymentStatus domain);

    @Mapping(target = "id", qualifiedByName = "stringToUUID")
    @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "ackPaymentSentId", qualifiedByName = "stringToUUID")
    PaymentStatus fromDto(PaymentStatusDto dto);

    @Mapping(target = "ackPaymentSent", ignore = true) // Update if nested mapping is available
    PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatusDto dto);

    PaymentStatusDto fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc);
}
