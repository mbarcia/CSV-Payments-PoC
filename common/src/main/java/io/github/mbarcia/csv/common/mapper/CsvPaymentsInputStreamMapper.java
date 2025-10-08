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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputStream;
import io.github.mbarcia.csv.common.dto.CsvPaymentsInputStreamDto;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@SuppressWarnings("unused")
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputStreamMapper extends io.github.mbarcia.pipeline.mapper.Mapper<InputCsvFileProcessingSvc.CsvPaymentsInputStream, CsvPaymentsInputStreamDto, CsvPaymentsInputStream> {

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
