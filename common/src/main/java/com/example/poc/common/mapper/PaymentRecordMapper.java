package com.example.poc.common.mapper;

import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.dto.PaymentRecordDto;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentRecordMapper {

  // Domain ↔ DTO
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "currency", qualifiedByName = "currencyToString")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath")
  PaymentRecordDto toDto(PaymentRecord entity);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath")
  PaymentRecord fromDto(PaymentRecordDto dto);

  // DTO ↔ gRPC
  InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecordDto dto);

  PaymentRecordDto fromGrpcToDto(InputCsvFileProcessingSvc.PaymentRecord grpc);

  // Domain ↔ DTO ↔ gRPC
  default InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord entity) {
    return toGrpc(toDto(entity));
  }

  default PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }
}
