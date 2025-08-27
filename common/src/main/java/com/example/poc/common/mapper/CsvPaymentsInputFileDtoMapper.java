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

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.dto.CsvPaymentsInputFileDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", uses = {CommonConverters.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputFileDtoMapper {

  CsvPaymentsInputFileDto toDto(CsvPaymentsInputFile entity);

  @Mapping(target = "records", ignore = true)
  @Mapping(target = "csvFile", ignore = true)
  CsvPaymentsInputFile fromDto(CsvPaymentsInputFileDto dto);
}