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

package test.package.test-step.service;

import test.package.common.domain.TestInput;
import test.package.common.domain.TestOutput;
import test.package.grpc.MutinyProcessTest StepServiceGrpc.MutinyProcessTest StepServiceStub;
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
    inputType = TestInput.class,
    outputType = TestOutput.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcServiceUnaryAdapter.class,
    grpcStub = MutinyProcessTest StepServiceGrpc.MutinyProcessTest StepServiceStub.class,
    grpcImpl = MutinyProcessTest StepServiceGrpc.ProcessTest StepServiceImplBase.class,
    inboundMapper = test.package.common.mapper.TestInputMapper.class,
    outboundMapper = test.package.common.mapper.TestOutputMapper.class,
    grpcClient = "TestStepSvc",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class ProcesstestStepService
    implements ReactiveUnaryService<TestInput, TestOutput> {

  @Override
  public Uni&lt;TestOutput&gt; process(TestInput input) {
    Logger logger = LoggerFactory.getLogger(getClass());

    // TODO implement business logic here
    logger.info("Processing input: {}", input);
    
    TestOutput output = new TestOutput();
    // Set output fields based on input
    // TODO: Add actual business logic here
    
    return Uni.createFrom().item(output);
  }
}