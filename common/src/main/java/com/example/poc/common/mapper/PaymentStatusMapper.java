package com.example.poc.common.mapper;

import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.dto.PaymentStatusDto;
import com.example.poc.grpc.PaymentsProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class, AckPaymentSentMapper.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper {

  // Domain ↔ DTO
  @Mapping(source = "id", target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "stringToUUID")
  @Mapping(target = "ackPaymentSent")
  PaymentStatusDto toDto(PaymentStatus entity);

  @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "fee", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "uuidToString")
  @Mapping(target = "ackPaymentSent")
  PaymentStatus fromDto(PaymentStatusDto dto);

  // DTO ↔ gRPC
  PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatusDto dto);

  PaymentStatusDto toDto(PaymentsProcessingSvc.PaymentStatus grpc);

  // Domain ↔ DTO ↔ gRPC
  default PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc) {
    return fromDto(toDto(grpc));
  }

  default PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus entity) {
    return toGrpc(toDto(entity));
  }
}
