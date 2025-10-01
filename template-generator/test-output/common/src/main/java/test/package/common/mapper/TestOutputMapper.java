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

package test.package.common.mapper;

import test.package.common.domain.TestOutput;
import test.package.common.dto.TestOutputDto;
import test.package.grpc.test-step-svc.TestOutput;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@SuppressWarnings("unused")
@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TestOutputMapper extends io.github.mbarcia.pipeline.mapper.Mapper<test.package.grpc.test-step-svc.TestOutput.TestOutput, TestOutputDto, TestOutput> {

  TestOutputMapper INSTANCE = Mappers.getMapper( TestOutputMapper.class );

  // Domain ↔ DTO
  @Override
  TestOutputDto toDto(TestOutput entity);

  @Override
  TestOutput fromDto(TestOutputDto dto);

  // DTO ↔ gRPC
  @Override
  test.package.grpc.test-step-svc.TestOutput.TestOutput toGrpc(TestOutputDto dto);

  @Override
  TestOutputDto fromGrpc(test.package.grpc.test-step-svc.TestOutput.TestOutput grpc);
}