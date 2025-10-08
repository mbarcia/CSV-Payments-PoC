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

package com.example.sample.common.mapper;

import com.example.sample.common.domain.ValidationOutput;
import com.example.sample.common.dto.ValidationOutputDto;
import com.example.sample.grpc.validate-order-svc.ValidationOutput;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@SuppressWarnings("unused")
@Mapper(
    componentModel = "jakarta",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ValidationOutputMapper extends io.github.mbarcia.pipeline.mapper.Mapper<com.example.sample.grpc.validate-order-svc.ValidationOutput.ValidationOutput, ValidationOutputDto, ValidationOutput> {

  ValidationOutputMapper INSTANCE = Mappers.getMapper( ValidationOutputMapper.class );

  // Domain ↔ DTO
  @Override
  ValidationOutputDto toDto(ValidationOutput entity);

  @Override
  ValidationOutput fromDto(ValidationOutputDto dto);

  // DTO ↔ gRPC
  @Override
  com.example.sample.grpc.validate-order-svc.ValidationOutput.ValidationOutput toGrpc(ValidationOutputDto dto);

  @Override
  ValidationOutputDto fromGrpc(com.example.sample.grpc.validate-order-svc.ValidationOutput.ValidationOutput grpc);
}