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

package com.example.sample.process-customer.service;

import com.example.sample.common.domain.CustomerInput;
import com.example.sample.common.domain.CustomerOutput;
import com.example.sample.grpc.MutinyProcessProcessCustomerServiceGrpc.MutinyProcessProcessCustomerServiceStub;
import io.github.mbarcia.pipeline.GenericGrpcServiceUnaryAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.service.ReactiveUnaryService;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PipelineStep(
    order = 1,
    inputType = com.example.sample.common.domain.CustomerInput.class,
    outputType = com.example.sample.common.domain.CustomerOutput.class,
    inputGrpcType = com.example.sample.grpc.process-customer.CustomerInput.class,
    outputGrpcType = com.example.sample.grpc.process-customer.CustomerOutput.class,
    stepType = io.github.mbarcia.pipeline.step.StepOneToOne.class,
    backendType = io.github.mbarcia.pipeline.GenericGrpcServiceUnaryAdapter.class,
    grpcStub = com.example.sample.grpc.process-customer.MutinyProcessProcessCustomerServiceGrpc.MutinyProcessProcessCustomerServiceStub.class,
    grpcImpl = com.example.sample.grpc.process-customer.MutinyProcessProcessCustomerServiceGrpc.ProcessProcessCustomerServiceImplBase.class,
    inboundMapper = com.example.sample.common.mapper.CustomerInputMapper.class,
    outboundMapper = com.example.sample.common.mapper.CustomerOutputMapper.class,
    grpcClient = "process-customer",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class ProcessprocessCustomerService
    implements ReactiveUnaryService<CustomerInput, CustomerOutput> {

  @Override
  public Uni&lt;CustomerOutput&gt; process(CustomerInput input) {
    Logger logger = LoggerFactory.getLogger(getClass());

    // TODO implement business logic here
    logger.info("Processing input: {}", input);
    
    CustomerOutput output = new CustomerOutput();
    // Set output fields based on input
    // TODO: Add actual business logic here
    
    return Uni.createFrom().item(output);
  }
}