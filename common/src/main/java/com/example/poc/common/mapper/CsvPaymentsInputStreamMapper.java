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

package com.example.poc.common.mapper;

import com.example.poc.common.domain.CsvPaymentsInputStream;
import com.example.poc.common.dto.CsvPaymentsInputStreamDto;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

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
