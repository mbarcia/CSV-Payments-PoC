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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputStream;
import io.github.mbarcia.csv.common.dto.CsvPaymentsInputStreamDto;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.annotation.MapperForStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@MapperForStep(
    order = 3,
    grpc = InputCsvFileProcessingSvc.CsvPaymentsInputStream.class,
    dto = CsvPaymentsInputStreamDto.class,
    domain = CsvPaymentsInputStream.class
)
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputStreamMapper {

  @Mapping(target = "source")
  CsvPaymentsInputStreamDto toDto(CsvPaymentsInputStream entity);

  @Mapping(target = "source")
  CsvPaymentsInputStream fromDto(CsvPaymentsInputStreamDto dto);

  @Mapping(target = "source")
  InputCsvFileProcessingSvc.CsvPaymentsInputStream toGrpc(CsvPaymentsInputStreamDto entity);

  @Mapping(target = "source")
  CsvPaymentsInputStreamDto fromGrpcToDto(InputCsvFileProcessingSvc.CsvPaymentsInputStream proto);

  // Domain ↔ DTO ↔ gRPC
  default InputCsvFileProcessingSvc.CsvPaymentsInputStream toGrpc(CsvPaymentsInputStream domain) {
    return toGrpc(toDto(domain));
  }

  default CsvPaymentsInputStream fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputStream grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }
}
