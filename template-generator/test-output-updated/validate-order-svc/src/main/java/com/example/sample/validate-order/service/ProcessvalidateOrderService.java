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

package com.example.sample.validate-order.service;

import com.example.sample.common.domain.CustomerOutput;
import com.example.sample.common.domain.ValidationOutput;
import com.example.sample.grpc.MutinyProcessValidateOrderServiceGrpc.MutinyProcessValidateOrderServiceStub;
import org.pipelineframework.GenericGrpcServiceUnaryAdapter;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.service.ReactiveUnaryService;
import org.pipelineframework.step.StepOneToOne;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PipelineStep(
    order = 2,
    inputType = com.example.sample.common.domain.CustomerOutput.class,
    outputType = com.example.sample.common.domain.ValidationOutput.class,
    inputGrpcType = com.example.sample.grpc.validate-order.CustomerOutput.class,
    outputGrpcType = com.example.sample.grpc.validate-order.ValidationOutput.class,
    stepType = org.pipelineframework.step.StepOneToOne.class,
    backendType = org.pipelineframework.GenericGrpcServiceUnaryAdapter.class,
    grpcStub = com.example.sample.grpc.validate-order.MutinyProcessValidateOrderServiceGrpc.MutinyProcessValidateOrderServiceStub.class,
    grpcImpl = com.example.sample.grpc.validate-order.MutinyProcessValidateOrderServiceGrpc.ProcessValidateOrderServiceImplBase.class,
    inboundMapper = com.example.sample.common.mapper.CustomerOutputMapper.class,
    outboundMapper = com.example.sample.common.mapper.ValidationOutputMapper.class,
    grpcClient = "validate-order",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class ProcessvalidateOrderService
    implements ReactiveUnaryService<CustomerOutput, ValidationOutput> {

  @Override
  public Uni&lt;ValidationOutput&gt; process(CustomerOutput input) {
    Logger logger = LoggerFactory.getLogger(getClass());

    // TODO implement business logic here
    logger.info("Processing input: {}", input);
    
    ValidationOutput output = new ValidationOutput();
    // Set output fields based on input
    // TODO: Add actual business logic here
    
    return Uni.createFrom().item(output);
  }
}