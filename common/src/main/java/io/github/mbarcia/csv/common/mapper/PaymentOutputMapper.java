/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mbarcia.csv.common.mapper;

import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.dto.PaymentOutputDto;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class, PaymentStatusMapper.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentOutputMapper {

  PaymentOutputMapper INSTANCE = Mappers.getMapper( PaymentOutputMapper.class );

  // Domain ↔ DTO
  @Mapping(target = "id")
  @Mapping(target = "amount")
  @Mapping(target = "currency")
  @Mapping(target = "fee")
  @Mapping(target = "paymentStatus")
  PaymentOutputDto toDto(PaymentOutput entity);

  @Mapping(target = "id")
  @Mapping(target = "amount")
  @Mapping(target = "currency")
  @Mapping(target = "fee")
  @Mapping(target = "paymentStatus")
  PaymentOutput fromDto(PaymentOutputDto dto);

  // DTO ↔ gRPC
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "currency", qualifiedByName = "currencyToString")
  @Mapping(target = "fee", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "paymentStatus")
  PaymentStatusSvc.PaymentOutput fromDtoToGrpc(PaymentOutputDto dto);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
  @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "paymentStatus")
  PaymentOutputDto fromGrpcToDto(PaymentStatusSvc.PaymentOutput grpc);

  // Domain ↔ DTO ↔ gRPC
  default PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }

  default PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput entity) {
    return fromDtoToGrpc(toDto(entity));
  }
}
