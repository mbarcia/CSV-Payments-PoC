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

import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.dto.PaymentStatusDto;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.annotation.MapperForStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperForStep(
    order = 7,
    grpc = PaymentsProcessingSvc.PaymentStatus.class,
    dto = PaymentStatusDto.class,
    domain = PaymentStatus.class
)
@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class, AckPaymentSentMapper.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper {

  PaymentStatusMapper INSTANCE = Mappers.getMapper( PaymentStatusMapper.class );

  // Domain ↔ DTO
  @Mapping(target = "id")
  @Mapping(target = "fee")
  @Mapping(target = "ackPaymentSentId")
  @Mapping(target = "ackPaymentSent")
  PaymentStatusDto toDto(PaymentStatus entity);

  @Mapping(target = "id")
  @Mapping(target = "fee")
  @Mapping(target = "ackPaymentSentId")
  @Mapping(target = "ackPaymentSent")
  PaymentStatus fromDto(PaymentStatusDto dto);

  // DTO ↔ gRPC
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "fee", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "uuidToString")
  @Mapping(target = "ackPaymentSent")
  PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatusDto dto);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "stringToUUID")
  @Mapping(target = "ackPaymentSent")
  PaymentStatusDto toDto(PaymentsProcessingSvc.PaymentStatus grpc);

  // Domain ↔ DTO ↔ gRPC
  default PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc) {
    return fromDto(toDto(grpc));
  }

  default PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus entity) {
    return toGrpc(toDto(entity));
  }
}
