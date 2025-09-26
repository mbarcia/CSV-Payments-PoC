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

package io.github.mbarcia.pipeline.processor;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
import io.github.mbarcia.pipeline.mapper.Mapper;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

/** Unit test for PipelineProcessor to validate annotation processing functionality and coverage. */
public class PipelineProcessorTest {

    @Test
    public void testPipelineStepAnnotationProcessing() {
        // Validate that the test service has the @PipelineStep annotation
        PipelineStep annotation = TestReactiveService.class.getAnnotation(PipelineStep.class);
        assertNotNull(annotation, "TestReactiveService should be annotated with @PipelineStep");

        // Check that all required annotation parameters are set properly
        assertEquals(1, annotation.order(), "Order should be 1");
        assertSame(annotation.stepType(), StepOneToOne.class, "Step type should be StepOneToOne");
        assertSame(
                annotation.backendType(),
                GenericGrpcReactiveServiceAdapter.class,
                "Backend type should be GenericGrpcReactiveServiceAdapter");
    }

    @Test
    public void testGenerateAdaptersBuildStep() throws Exception {
        // Create index with our test service
        Index index =
                Index.of(
                        TestReactiveService.class,
                        TestGrpcStub.class,
                        TestMapper.class,
                        TestOutboundMapper.class);

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, false);

        // Verify that generated gRPC adapter classes were produced when generateCli is false
        assertTrue(
                generatedClassesProducer.items.size() >= 1,
                "Should generate gRPC adapter when generateCli is false");
    }

    @Test
    public void testGenerateAdaptersWithMultipleServices() throws Exception {
        // Create index with multiple services
        Index index = Index.of(TestReactiveService.class, TestOneToManyService.class);

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, false);

        // Should generate 2 gRPC adapter classes: 2 services * 1 adapter = 2
        assertTrue(
                generatedClassesProducer.items.size() >= 2,
                "Should generate gRPC adapter for each service when generateCli is false");
    }

    @Test
    public void testGenerateAdaptersWithNoAnnotations() throws Exception {
        // Create index without any @PipelineStep annotations
        Index index = Index.of(TestGrpcStub.class); // Only include classes without @PipelineStep

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, false);

        // Should not generate any classes since there are no @PipelineStep annotations
        assertEquals(
                0,
                generatedClassesProducer.items.size(),
                "Should not generate classes when there are no @PipelineStep annotations");
    }

    @Test
    public void testGeneratePipelineApplication() throws Exception {
        // Create index with multiple services to trigger application generation
        Index index = Index.of(TestReactiveService.class, TestOneToManyService.class);

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, true);

        // Should generate step classes, and the pipeline application (when generateCli=true)
        // With 2 services: 2 steps + 1 application = 3 classes at minimum
        assertTrue(
                generatedClassesProducer.items.size() >= 3,
                "Should generate step classes and pipeline application");

        // Check that the GeneratedPipelineApplication class was created
        boolean hasGeneratedApp =
                generatedClassesProducer.items.stream()
                        .anyMatch(item -> item.getName().contains("GeneratedPipelineApplication"));
        assertTrue(hasGeneratedApp, "Should generate GeneratedPipelineApplication class");
    }

    @Test
    public void testGeneratePipelineApplicationWithSingleService() throws Exception {
        // Create index with single service
        Index index = Index.of(TestReactiveService.class);

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, true);

        // Should generate step class and the pipeline application (when generateCli=true)
        // With 1 service: 1 step + 1 application = 2 classes
        assertTrue(
                generatedClassesProducer.items.size() >= 2,
                "Should generate step class and pipeline application for single service");

        // Check that the GeneratedPipelineApplication class was created
        boolean hasGeneratedApp =
                generatedClassesProducer.items.stream()
                        .anyMatch(item -> item.getName().contains("GeneratedPipelineApplication"));
        assertTrue(
                hasGeneratedApp,
                "Should generate GeneratedPipelineApplication class for single service");
    }

    private static DummyGeneratedClassBuildProducer getDummyGeneratedClassBuildProducer(
            Index index, boolean x) {
        CombinedIndexBuildItem indexBuildItem = new CombinedIndexBuildItem(index, index);
        DummySyntheticBeanBuildProducer beansProducer = new DummySyntheticBeanBuildProducer();
        DummyGeneratedClassBuildProducer generatedClassesProducer =
                new DummyGeneratedClassBuildProducer();
        PipelineBuildTimeConfig config =
                new PipelineBuildTimeConfig() {
                    @Override
                    public Boolean generateCli() {
                        return x;
                    }

                    @Override
                    public String version() {
                        return "";
                    }
                };

        // Execute both build steps
        PipelineProcessor processor = new PipelineProcessor();
        processor.generateAdapters(indexBuildItem, config, beansProducer, generatedClassesProducer);

        // Also execute the CLI application generation step if needed
        processor.generateCliApplication(indexBuildItem, config, generatedClassesProducer);
        return generatedClassesProducer;
    }

    // Test classes
    @PipelineStep(
            order = 1,
            inputType = String.class,
            outputType = Integer.class,
            stepType = StepOneToOne.class,
            backendType = GenericGrpcReactiveServiceAdapter.class,
            grpcStub = TestGrpcStub.class,
            grpcImpl = TestGrpcImpl.class,
            inboundMapper = TestMapper.class,
            outboundMapper = TestOutboundMapper.class,
            grpcClient = "test-client",
            autoPersist = true,
            debug = true)
    @ApplicationScoped
    public static class TestReactiveService implements ReactiveService<String, Integer> {
        @Override
        public Uni<Integer> process(String input) {
            return Uni.createFrom().item(input.length());
        }
    }

    @PipelineStep(
            order = 2,
            inputType = String.class,
            outputType = Integer.class,
            stepType = StepOneToMany.class,
            backendType = GenericGrpcReactiveServiceAdapter.class,
            grpcStub = TestGrpcStub.class,
            grpcImpl = TestGrpcImpl.class,
            inboundMapper = TestMapper.class,
            outboundMapper = TestOutboundMapper.class,
            grpcClient = "test-client-many",
            autoPersist = true,
            debug = true)
    @ApplicationScoped
    public static class TestOneToManyService implements ReactiveService<String, Integer> {
        @Override
        public Uni<Integer> process(String input) {
            return Uni.createFrom().item(input.length());
        }
    }

    // Supporting mock classes
    public static class TestGrpcStub {
        public Uni<Integer> remoteProcess(String input) {
            return Uni.createFrom().item(input.length());
        }
    }

    public static class TestGrpcImpl {
        // Placeholder gRPC implementation class
    }

    public static class TestMapper implements Mapper<String, String, String> {
        @Override
        public String fromGrpcFromDto(String grpcIn) {
            return null;
        }

        @Override
        public String toDtoToGrpc(String grpcInput) {
            return grpcInput;
        }

        @Override
        public String fromGrpc(String s) {
            return null;
        }

        @Override
        public String toGrpc(String s) {
            return null;
        }

        @Override
        public String fromDto(String s) {
            return null;
        }

        @Override
        public String toDto(String s) {
            return null;
        }
    }

    public static class TestOutboundMapper implements Mapper<Integer, Integer, Integer> {
        @Override
        public Integer fromGrpcFromDto(Integer grpcIn) {
            return null;
        }

        @Override
        public Integer toDtoToGrpc(Integer domainOutput) {
            return domainOutput;
        }

        @Override
        public Integer fromGrpc(Integer integer) {
            return null;
        }

        @Override
        public Integer toGrpc(Integer integer) {
            return null;
        }

        @Override
        public Integer fromDto(Integer integer) {
            return null;
        }

        @Override
        public Integer toDto(Integer integer) {
            return null;
        }
    }

    // Specific dummy implementations for testing
    static class DummySyntheticBeanBuildProducer
            implements io.quarkus.deployment.annotations.BuildProducer<
                    io.quarkus.arc.deployment.SyntheticBeanBuildItem> {
        final java.util.List<io.quarkus.arc.deployment.SyntheticBeanBuildItem> items =
                new java.util.ArrayList<>();

        @Override
        public void produce(io.quarkus.arc.deployment.SyntheticBeanBuildItem item) {
            items.add(item);
        }
    }

    static class DummyGeneratedClassBuildProducer
            implements io.quarkus.deployment.annotations.BuildProducer<
                    io.quarkus.deployment.builditem.GeneratedClassBuildItem> {
        final java.util.List<io.quarkus.deployment.builditem.GeneratedClassBuildItem> items =
                new java.util.ArrayList<>();

        @Override
        public void produce(io.quarkus.deployment.builditem.GeneratedClassBuildItem item) {
            items.add(item);
        }
    }
}
