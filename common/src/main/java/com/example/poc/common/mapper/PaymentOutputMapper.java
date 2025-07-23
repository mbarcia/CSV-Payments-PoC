package com.example.poc.common.mapper;

import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.dto.PaymentOutputDto;
import com.example.poc.grpc.PaymentStatusSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentOutputMapper {

  // Domain ↔ DTO
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "currency", qualifiedByName = "currencyToString")
  @Mapping(source = "fee", target = "fee", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "paymentStatus")
  PaymentOutputDto toDto(PaymentOutput entity);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
  @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "paymentStatus")
  PaymentOutput fromDto(PaymentOutputDto dto);

  // DTO ↔ gRPC
  PaymentStatusSvc.PaymentOutput fromDtoToGrpc(PaymentOutputDto dto);

  PaymentOutputDto fromGrpcToDto(PaymentStatusSvc.PaymentOutput grpc);

  // Domain ↔ DTO ↔ gRPC
  default PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }

  default PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput entity) {
    return fromDtoToGrpc(toDto(entity));
  }
}
