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
import java.util.Optional;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

/** Unit test for PipelineProcessor to validate annotation processing functionality and coverage. */
@SuppressWarnings("removal")
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
        assertFalse(
                generatedClassesProducer.items.isEmpty(),
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
    public void testGeneratePipelineApplicationWithSingleService() throws Exception {
        // Create index with single service
        Index index = Index.of(TestReactiveService.class);

        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, true);

        // Should generate step class (when isOrchestrator() == true)
        // With 1 service: 1 step = 1 class
        assertEquals(
                1,
                generatedClassesProducer.items.size(),
                "Should generate step class for single service");
    }

    @Test
    public void testLocalPropertyDefault() {
        // Validate that the local property defaults to false
        PipelineStep annotation = TestReactiveService.class.getAnnotation(PipelineStep.class);
        assertNotNull(annotation, "TestReactiveService should be annotated with @PipelineStep");

        // By default, local should be false for backward compatibility
        assertFalse(annotation.local(), "Local property should default to false");
    }

    @Test
    public void testLocalPropertyTrue() {
        // Validate that the local property can be set to true
        PipelineStep annotation = TestLocalReactiveService.class.getAnnotation(PipelineStep.class);
        assertNotNull(
                annotation, "TestLocalReactiveService should be annotated with @PipelineStep");

        // For local services, it should be true
        assertTrue(annotation.local(), "Local property should be true when explicitly set");
    }

    @Test
    public void testLocalStepAnnotationProcessing() {
        // Validate that local service has all required properties
        PipelineStep annotation = TestLocalReactiveService.class.getAnnotation(PipelineStep.class);
        assertNotNull(
                annotation, "TestLocalReactiveService should be annotated with @PipelineStep");

        // Check that all required annotation parameters are set properly for local step
        assertEquals(1, annotation.order(), "Order should be 1");
        assertSame(annotation.stepType(), StepOneToMany.class, "Step type should be StepOneToMany");
        assertTrue(annotation.local(), "Local property should be true");
        assertSame(annotation.grpcImpl(), Void.class, "grpcImpl should be Void for local steps");
    }

    @Test
    public void testLocalStepGenerationWhenGenerateCliIsTrue() throws Exception {
        // Create index with local service
        Index index = Index.of(TestLocalReactiveService.class);

        // Generate when generateCli=true (for client-side generation)
        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, true);

        // Should generate local step class when generateCli=true and local=true
        assertFalse(
                generatedClassesProducer.items.isEmpty(),
                "Should generate local step class when generateCli is true and step is local");

        // Check that a step class was generated (it should have "Step" in the name)
        boolean hasStepClass =
                generatedClassesProducer.items.stream()
                        .anyMatch(item -> item.getName().contains("TestLocalReactiveServiceStep"));
        assertTrue(hasStepClass, "Should generate step class for local service with proper naming");
    }

    @Test
    public void testLocalStepDoesNotGenerateGrpcAdapterWhenGenerateCliIsFalse() throws Exception {
        // Create index with local service
        Index index = Index.of(TestLocalReactiveService.class);

        // Generate when generateCli=false (for server-side generation)
        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, false);

        // For local steps and generateCli=false, no gRPC adapter should be generated
        // The test service has local=true, so no gRPC adapter should be created
        // We expect fewer generated classes compared to non-local services
        long grpcAdapterCount =
                generatedClassesProducer.items.stream()
                        .filter(item -> item.getName().contains("GrpcService"))
                        .count();

        // Local steps should not generate gRPC service adapters
        assertEquals(0, grpcAdapterCount, "Local steps should not generate gRPC service adapters");
    }

    @Test
    public void testMixedLocalAndRemoteSteps() throws Exception {
        // Create index with both local and remote services
        Index index = Index.of(TestReactiveService.class, TestLocalReactiveService.class);

        // Generate when generateCli=true
        DummyGeneratedClassBuildProducer generatedClassesProducer =
                getDummyGeneratedClassBuildProducer(index, true);

        // Should generate step classes for both local and remote services
        assertTrue(
                generatedClassesProducer.items.size() >= 2,
                "Should generate step classes for both local and remote services");

        // Check that both step classes were generated
        boolean hasRemoteStep =
                generatedClassesProducer.items.stream()
                        .anyMatch(item -> item.getName().contains("TestReactiveServiceStep"));
        boolean hasLocalStep =
                generatedClassesProducer.items.stream()
                        .anyMatch(item -> item.getName().contains("TestLocalReactiveServiceStep"));

        assertTrue(hasRemoteStep, "Should generate step class for remote service");
        assertTrue(hasLocalStep, "Should generate step class for local service");
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

                    @Override
                    public Optional<String> cliName() {
                        return Optional.of("test-cli");
                    }

                    @Override
                    public Optional<String> cliDescription() {
                        return Optional.of("Test CLI application");
                    }

                    @Override
                    public Optional<String> cliVersion() {
                        return Optional.of("1.0.0");
                    }
                };

        // Execute both build steps
        PipelineProcessor processor = new PipelineProcessor();
        processor.generateAdapters(indexBuildItem, config, beansProducer, generatedClassesProducer);

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

    // Local service test class - this represents a service that runs locally without
    // gRPC
    @PipelineStep(
            order = 1,
            inputType = String.class,
            outputType = Integer.class,
            stepType = StepOneToMany.class,
            backendType =
                    GenericGrpcReactiveServiceAdapter.class, // This won't be used for local steps
            // No gRPC stub for local steps
            grpcImpl = Void.class, // No gRPC impl for local steps
            inboundMapper = TestMapper.class,
            outboundMapper = TestOutboundMapper.class,
            // No gRPC client for local steps
            local = true) // This is the key difference - local step
    @ApplicationScoped
    public static class TestLocalReactiveService implements ReactiveService<String, Integer> {
        @Override
        public Uni<Integer> process(String input) {
            return Uni.createFrom().item(input.length());
        }
    }

    // Supporting mock classes
    public static class TestGrpcStub {}

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
