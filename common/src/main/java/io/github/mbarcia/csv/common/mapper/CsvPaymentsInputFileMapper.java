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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.dto.CsvPaymentsInputFileDto;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.annotation.MapperForStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperForStep(
    order = 2,
    grpc = InputCsvFileProcessingSvc.CsvPaymentsInputFile.class,
    dto = CsvPaymentsInputFileDto.class,
    domain = CsvPaymentsInputFile.class
)
@Mapper(componentModel = "cdi", uses = {CommonConverters.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvPaymentsInputFileMapper {

  CsvPaymentsInputFileMapper INSTANCE = Mappers.getMapper( CsvPaymentsInputFileMapper.class );

  @Mapping(target = "id")
  @Mapping(target = "filepath")
  @Mapping(target = "csvFolderPath")
  CsvPaymentsInputFileDto toDto(CsvPaymentsInputFile entity);

  @Mapping(target = "id")
  @Mapping(target = "filepath")
  @Mapping(target = "csvFolderPath")
  CsvPaymentsInputFile fromDto(CsvPaymentsInputFileDto dto);

  @Mapping(target = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "filepath", qualifiedByName = "pathToString")
  @Mapping(target = "csvFolderPath", qualifiedByName = "pathToString")
  InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFileDto entity);

  @Mapping(target = "id", qualifiedByName = "stringToUUID")
  @Mapping(target = "filepath", qualifiedByName = "stringToPath")
  @Mapping(target = "csvFolderPath", qualifiedByName = "stringToPath")
  CsvPaymentsInputFileDto fromGrpcToDto(InputCsvFileProcessingSvc.CsvPaymentsInputFile proto);

  // Domain ↔ DTO ↔ gRPC
  default InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFile domain) {
    return toGrpc(toDto(domain));
  }

  default CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile grpc) {
    return fromDto(fromGrpcToDto(grpc));
  }
}
