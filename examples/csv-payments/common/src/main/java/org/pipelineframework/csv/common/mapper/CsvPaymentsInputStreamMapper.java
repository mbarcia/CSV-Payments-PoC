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

package org.pipelineframework.csv.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.pipelineframework.csv.common.domain.CsvPaymentsInputStream;
import org.pipelineframework.csv.common.dto.CsvPaymentsInputStreamDto;
import org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc;

@SuppressWarnings("unused")
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputStreamMapper extends org.pipelineframework.mapper.Mapper<InputCsvFileProcessingSvc.CsvPaymentsInputStream, CsvPaymentsInputStreamDto, CsvPaymentsInputStream> {

  CsvPaymentsInputStreamMapper INSTANCE = Mappers.getMapper( CsvPaymentsInputStreamMapper.class );

  @Override
  @Mapping(target = "source")
  CsvPaymentsInputStreamDto toDto(CsvPaymentsInputStream entity);

  @Override
  @Mapping(target = "source")
  CsvPaymentsInputStream fromDto(CsvPaymentsInputStreamDto dto);

  @Override
  @Mapping(target = "source")
  InputCsvFileProcessingSvc.CsvPaymentsInputStream toGrpc(CsvPaymentsInputStreamDto entity);

  @Override
  @Mapping(target = "source")
  CsvPaymentsInputStreamDto fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputStream proto);
}
