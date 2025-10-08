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

import com.example.sample.common.domain.CustomerOutput;
import com.example.sample.common.dto.CustomerOutputDto;
import com.example.sample.grpc.validate-order-svc.CustomerOutput;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@SuppressWarnings("unused")
@Mapper(
    componentModel = "jakarta",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CustomerOutputMapper extends io.github.mbarcia.pipeline.mapper.Mapper<com.example.sample.grpc.validate-order-svc.CustomerOutput.CustomerOutput, CustomerOutputDto, CustomerOutput> {

  CustomerOutputMapper INSTANCE = Mappers.getMapper( CustomerOutputMapper.class );

  // Domain ↔ DTO
  @Override
  CustomerOutputDto toDto(CustomerOutput entity);

  @Override
  CustomerOutput fromDto(CustomerOutputDto dto);

  // DTO ↔ gRPC
  @Override
  com.example.sample.grpc.validate-order-svc.CustomerOutput.CustomerOutput toGrpc(CustomerOutputDto dto);

  @Override
  CustomerOutputDto fromGrpc(com.example.sample.grpc.validate-order-svc.CustomerOutput.CustomerOutput grpc);
}