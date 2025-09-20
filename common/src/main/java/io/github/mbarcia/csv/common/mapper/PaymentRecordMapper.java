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

import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.dto.PaymentRecordDto;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentRecordMapper {

  PaymentRecordMapper INSTANCE = Mappers.getMapper( PaymentRecordMapper.class );

  // Domain ↔ DTO
  @Mapping(target = "id")
  @Mapping(target = "amount")
  @Mapping(target = "currency")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath")
  PaymentRecordDto toDto(PaymentRecord entity);

  @Mapping(target = "id")
  @Mapping(target = "amount")
  @Mapping(target = "currency")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath")
  PaymentRecord fromDto(PaymentRecordDto dto);

  // DTO ↔ gRPC
  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
  @Mapping(target = "currency", qualifiedByName = "currencyToString")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath", qualifiedByName = "pathToString")
  InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecordDto dto);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
  @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
  @Mapping(target = "csvId")
  @Mapping(target = "csvPaymentsInputFilePath", qualifiedByName = "stringToPath")
  PaymentRecordDto fromGrpcToDto(InputCsvFileProcessingSvc.PaymentRecord grpc);

  // Domain ↔ DTO ↔ gRPC
  default InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord entity) {
    return toGrpc(toDto(entity));
  }

  default PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }
}
