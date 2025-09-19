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

import io.github.mbarcia.csv.common.domain.CsvFolder;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.annotation.MapperForStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperForStep(
    order = 1,
    grpc = InputCsvFileProcessingSvc.CsvFolder.class,
    domain = CsvFolder.class
)
@Mapper(componentModel = "cdi", uses = {CommonConverters.class}, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CsvFolderMapper {

  CsvFolderMapper INSTANCE = Mappers.getMapper( CsvFolderMapper.class );

  @Mapping(target = "folderPath", qualifiedByName = "pathToString")
  InputCsvFileProcessingSvc.CsvFolder toGrpc(CsvFolder entity);

  @Mapping(target = "folderPath", qualifiedByName = "stringToPath")
  CsvFolder fromGrpc(InputCsvFileProcessingSvc.CsvFolder proto);
}