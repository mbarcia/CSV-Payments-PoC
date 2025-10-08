/*
 * Copyright (c) 2023-2025 Mariano Barcia
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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@SuppressWarnings("unused")
@Mapper(
    componentModel = "jakarta",
    uses = {CommonConverters.class, AckPaymentSentMapper.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentStatusMapper extends io.github.mbarcia.pipeline.mapper.Mapper<PaymentsProcessingSvc.PaymentStatus, PaymentStatusDto, PaymentStatus> {

  PaymentStatusMapper INSTANCE = Mappers.getMapper( PaymentStatusMapper.class );

  // Domain ↔ DTO
  @Override
  PaymentStatusDto toDto(PaymentStatus entity);

  @Override
  PaymentStatus fromDto(PaymentStatusDto dto);

  // DTO ↔ gRPC
  @Override
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "fee", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "uuidToString")
  PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatusDto dto);

  @Override
  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "fee", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "ackPaymentSentId", qualifiedByName = "stringToUUID")
  PaymentStatusDto fromGrpc(PaymentsProcessingSvc.PaymentStatus grpc);
}
