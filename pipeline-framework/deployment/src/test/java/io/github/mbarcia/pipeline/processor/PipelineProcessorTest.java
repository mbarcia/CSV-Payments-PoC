/// *
// * Copyright (c) 2023-2025 Mariano Barcia
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package io.github.mbarcia.pipeline.processor;
//
// import static org.junit.jupiter.api.Assertions.*;
//
// import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
// import io.github.mbarcia.pipeline.annotation.PipelineStep;
// import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
// import io.github.mbarcia.pipeline.mapper.Mapper;
// import io.github.mbarcia.pipeline.service.ReactiveService;
// import io.github.mbarcia.pipeline.step.StepOneToMany;
// import io.github.mbarcia.pipeline.step.StepOneToOne;
// import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
// import io.quarkus.deployment.annotations.BuildProducer;
// import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
// import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
// import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
// import io.smallrye.mutiny.Uni;
// import jakarta.enterprise.context.ApplicationScoped;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;
// import org.junit.jupiter.api.Test;
//
/// ** Unit test for PipelineProcessor to validate annotation processing functionality and coverage.
// */
// @SuppressWarnings("removal")
// public class PipelineProcessorTest {
//
//    private BuildSystemTargetBuildItem createBuildTarget() {
//        java.nio.file.Path targetPath = java.nio.file.Paths.get("target");
//        return new BuildSystemTargetBuildItem(targetPath, "test", false, null);
//    }
//
//    @Test
//    public void testPipelineStepAnnotationProcessing() {
//        // Validate that the test service has the @PipelineStep annotation
//        PipelineStep annotation = TestReactiveService.class.getAnnotation(PipelineStep.class);
//        assertNotNull(annotation, "TestReactiveService should be annotated with @PipelineStep");
//
//        // Check that all required annotation parameters are set properly
//        assertEquals(1, annotation.order(), "Order should be 1");
//        assertSame(annotation.stepType(), StepOneToOne.class, "Step type should be StepOneToOne");
//        assertSame(
//                annotation.backendType(),
//                GenericGrpcReactiveServiceAdapter.class,
//                "Backend type should be GenericGrpcReactiveServiceAdapter");
//    }
//
//    @Test
//    public void testGenerateStepsRegistryBuildStep() throws Exception {
//        // Test the generation of StepsRegistryImpl when generateCli is true
//        DummyGeneratedClassBuildProducer generatedClassesProducer =
//                new DummyGeneratedClassBuildProducer();
//        DummyGeneratedResourceBuildProducer generatedResourceBuildProducer =
//                new DummyGeneratedResourceBuildProducer();
//
//        PipelineBuildTimeConfig config =
//                new PipelineBuildTimeConfig() {
//                    @Override
//                    public Boolean generateCli() {
//                        return true; // generateCli is true
//                    }
//
//                    @Override
//                    public String version() {
//                        return "";
//                    }
//
//                    @Override
//                    public Optional<String> cliName() {
//                        return Optional.of("test-cli");
//                    }
//
//                    @Override
//                    public Optional<String> cliDescription() {
//                        return Optional.of("Test CLI application");
//                    }
//
//                    @Override
//                    public Optional<String> cliVersion() {
//                        return Optional.of("1.0.0");
//                    }
//                };
//
//        // Execute the generateStepsRegistry build step
//        PipelineProcessor processor = new PipelineProcessor();
//        BuildProducer<ReflectiveClassBuildItem> reflectiveClasses =
//                new DummyReflectiveClassBuildItemBuildProducer();
//
//        processor.generateStepsRegistry(
//                config,
//                generatedClassesProducer,
//                generatedResourceBuildProducer,
//                reflectiveClasses);
//
//        // Verify that StepsRegistryImpl was generated when generateCli is true
//        assertTrue(
//                generatedClassesProducer.items.stream()
//                        .anyMatch(item -> item.getName().contains("StepsRegistryImpl")),
//                "Should generate StepsRegistryImpl when generateCli is true");
//    }
//
//    @Test
//    public void testGenerateStepsRegistryWithGenerateCliFalse() throws Exception {
//        // Test that StepsRegistryImpl is not generated when generateCli is false
//        DummyGeneratedClassBuildProducer generatedClassesProducer =
//                new DummyGeneratedClassBuildProducer();
//        DummyGeneratedResourceBuildProducer generatedResourceBuildProducer =
//                new DummyGeneratedResourceBuildProducer();
//
//        PipelineBuildTimeConfig config =
//                new PipelineBuildTimeConfig() {
//                    @Override
//                    public Boolean generateCli() {
//                        return false; // generateCli is false
//                    }
//
//                    @Override
//                    public String version() {
//                        return "";
//                    }
//
//                    @Override
//                    public Optional<String> cliName() {
//                        return Optional.of("test-cli");
//                    }
//
//                    @Override
//                    public Optional<String> cliDescription() {
//                        return Optional.of("Test CLI application");
//                    }
//
//                    @Override
//                    public Optional<String> cliVersion() {
//                        return Optional.of("1.0.0");
//                    }
//                };
//
//        // Execute the generateStepsRegistry build step
//        PipelineProcessor processor = new PipelineProcessor();
//        BuildProducer<ReflectiveClassBuildItem> reflectiveClasses =
//                new DummyReflectiveClassBuildItemBuildProducer();
//
//        processor.generateStepsRegistry(
//                config,
//                generatedClassesProducer,
//                generatedResourceBuildProducer,
//                reflectiveClasses);
//
//        // Verify that StepsRegistryImpl was NOT generated when generateCli is false
//        assertFalse(
//                generatedClassesProducer.items.stream()
//                        .anyMatch(item -> item.getName().contains("StepsRegistryImpl")),
//                "Should NOT generate StepsRegistryImpl when generateCli is false");
//    }
//
//    @Test
//    public void testGenerateStepsRegistryEmptyList() throws Exception {
//        // Test the generation of StepsRegistryImpl with an empty list of step classes
//        DummyGeneratedClassBuildProducer generatedClassesProducer =
//                new DummyGeneratedClassBuildProducer();
//        DummyGeneratedResourceBuildProducer generatedResourceBuildProducer =
//                new DummyGeneratedResourceBuildProducer();
//
//        PipelineBuildTimeConfig config =
//                new PipelineBuildTimeConfig() {
//                    @Override
//                    public Boolean generateCli() {
//                        return true; // generateCli is true
//                    }
//
//                    @Override
//                    public String version() {
//                        return "";
//                    }
//
//                    @Override
//                    public Optional<String> cliName() {
//                        return Optional.of("test-cli");
//                    }
//
//                    @Override
//                    public Optional<String> cliDescription() {
//                        return Optional.of("Test CLI application");
//                    }
//
//                    @Override
//                    public Optional<String> cliVersion() {
//                        return Optional.of("1.0.0");
//                    }
//                };
//
//        // Execute the generateStepsRegistry build step
//        PipelineProcessor processor = new PipelineProcessor();
//        BuildProducer<ReflectiveClassBuildItem> reflectiveClasses =
//                new DummyReflectiveClassBuildItemBuildProducer();
//
//        processor.generateStepsRegistry(
//                config,
//                generatedClassesProducer,
//                generatedResourceBuildProducer,
//                reflectiveClasses);
//
//        // Verify that StepsRegistryImpl was generated even with an empty list of step classes
//        assertTrue(
//                generatedClassesProducer.items.stream()
//                        .anyMatch(item -> item.getName().contains("StepsRegistryImpl")),
//                "Should generate StepsRegistryImpl even with empty step class list");
//    }
//
//    @Test
//    public void testGenerateStepsRegistryConditionalGeneration() throws Exception {
//        // Test that StepsRegistryImpl is only generated when generateCli is true
//        DummyGeneratedClassBuildProducer generatedClassesProducer =
//                new DummyGeneratedClassBuildProducer();
//        DummyGeneratedResourceBuildProducer generatedResourceBuildProducer =
//                new DummyGeneratedResourceBuildProducer();
//
//        PipelineBuildTimeConfig config =
//                new PipelineBuildTimeConfig() {
//                    @Override
//                    public Boolean generateCli() {
//                        return true; // generateCli is true
//                    }
//
//                    @Override
//                    public String version() {
//                        return "";
//                    }
//
//                    @Override
//                    public Optional<String> cliName() {
//                        return Optional.of("test-cli");
//                    }
//
//                    @Override
//                    public Optional<String> cliDescription() {
//                        return Optional.of("Test CLI application");
//                    }
//
//                    @Override
//                    public Optional<String> cliVersion() {
//                        return Optional.of("1.0.0");
//                    }
//                };
//
//        // Execute the generateStepsRegistry build step
//        PipelineProcessor processor = new PipelineProcessor();
//        BuildProducer<ReflectiveClassBuildItem> reflectiveClasses =
//                new DummyReflectiveClassBuildItemBuildProducer();
//
//        processor.generateStepsRegistry(
//                config,
//                generatedClassesProducer,
//                generatedResourceBuildProducer,
//                reflectiveClasses);
//
//        // Verify that StepsRegistryImpl was generated when generateCli is true
//        boolean hasStepsRegistry =
//                generatedClassesProducer.items.stream()
//                        .anyMatch(item -> item.getName().contains("StepsRegistryImpl"));
//
//        assertTrue(
//                hasStepsRegistry,
//                "Should generate StepsRegistryImpl class when generateCli is true");
//    }
//
//    @Test
//    public void testLocalPropertyDefault() {
//        // Validate that the local property defaults to false
//        PipelineStep annotation = TestReactiveService.class.getAnnotation(PipelineStep.class);
//        assertNotNull(annotation, "TestReactiveService should be annotated with @PipelineStep");
//
//        // By default, local should be false for backward compatibility
//        assertFalse(annotation.local(), "Local property should default to false");
//    }
//
//    @Test
//    public void testLocalPropertyTrue() {
//        // Validate that the local property can be set to true
//        PipelineStep annotation =
// TestLocalReactiveService.class.getAnnotation(PipelineStep.class);
//        assertNotNull(
//                annotation, "TestLocalReactiveService should be annotated with @PipelineStep");
//
//        // For local services, it should be true
//        assertTrue(annotation.local(), "Local property should be true when explicitly set");
//    }
//
//    @Test
//    public void testLocalStepAnnotationProcessing() {
//        // Validate that local service has all required properties
//        PipelineStep annotation =
// TestLocalReactiveService.class.getAnnotation(PipelineStep.class);
//        assertNotNull(
//                annotation, "TestLocalReactiveService should be annotated with @PipelineStep");
//
//        // Check that all required annotation parameters are set properly for local step
//        assertEquals(1, annotation.order(), "Order should be 1");
//        assertSame(annotation.stepType(), StepOneToMany.class, "Step type should be
// StepOneToMany");
//        assertTrue(annotation.local(), "Local property should be true");
//        assertSame(annotation.grpcImpl(), Void.class, "grpcImpl should be Void for local steps");
//    }
//
//    @Test
//    public void testPipelineProcessorFeatureBuildStep() {
//        // Test the feature build step
//        PipelineProcessor processor = new PipelineProcessor();
//        io.quarkus.deployment.builditem.FeatureBuildItem feature = processor.feature();
//
//        assertNotNull(feature, "FeatureBuildItem should not be null");
//        assertEquals("pipeline-framework", feature.getName(), "Feature name should match");
//    }
//
//    @Test
//    public void testStepsRegistrySourceCodeGeneration() {
//        // Test that the source code for StepsRegistryImpl is generated correctly
//        List<String> stepClassNames =
//                List.of("io.github.mbarcia.test.Step1", "io.github.mbarcia.test.Step2");
//
//        String sourceCode =
//                PipelineProcessor.generateStepsRegistrySourceCode(
//                        "io.github.mbarcia.pipeline.generated",
//                        "StepsRegistryImpl",
//                        stepClassNames);
//
//        // Verify that the generated source contains expected elements
//        assertTrue(sourceCode.contains("package io.github.mbarcia.pipeline.generated;"));
//        assertTrue(sourceCode.contains("public class StepsRegistryImpl"));
//        assertTrue(sourceCode.contains("java.util.List"));
//        assertTrue(sourceCode.contains("java.util.ArrayList"));
//        assertTrue(sourceCode.contains("io.github.mbarcia.test.Step1"));
//        assertTrue(sourceCode.contains("io.github.mbarcia.test.Step2"));
//        assertTrue(sourceCode.contains("private static final String[] STEP_CLASS_NAMES"));
//        assertTrue(sourceCode.contains("getSteps()"));
//    }
//
//    @Test
//    public void testStepsRegistrySourceCodeGenerationWithEmptyList() {
//        // Test that the source code for StepsRegistryImpl is generated correctly with empty list
//        List<String> stepClassNames = List.of(); // Empty list
//
//        String sourceCode =
//                PipelineProcessor.generateStepsRegistrySourceCode(
//                        "io.github.mbarcia.pipeline.generated",
//                        "StepsRegistryImpl",
//                        stepClassNames);
//
//        // Verify that the generated source contains expected elements for empty list
//        assertTrue(sourceCode.contains("package io.github.mbarcia.pipeline.generated;"));
//        assertTrue(sourceCode.contains("public class StepsRegistryImpl"));
//        assertTrue(
//                sourceCode.contains(
//                        "private static final String[] STEP_CLASS_NAMES = new String[0];"));
//        assertTrue(sourceCode.contains("getSteps()"));
//        assertFalse(
//                sourceCode.contains(
//                        "STEP_CLASS_NAMES = new String[] {")); // Should not have populated array
//    }
//
//    // Test classes
//    @PipelineStep(
//            order = 1,
//            inputType = String.class,
//            outputType = Integer.class,
//            stepType = io.github.mbarcia.pipeline.step.StepOneToOne.class,
//            backendType = GenericGrpcReactiveServiceAdapter.class,
//            grpcStub = TestGrpcStub.class,
//            grpcImpl = TestGrpcImpl.class,
//            inboundMapper = TestMapper.class,
//            outboundMapper = TestOutboundMapper.class,
//            grpcClient = "test-client",
//            autoPersist = true,
//            debug = true)
//    @ApplicationScoped
//    public static class TestReactiveService implements ReactiveService<String, Integer> {
//        @Override
//        public Uni<Integer> process(String input) {
//            return Uni.createFrom().item(input.length());
//        }
//    }
//
//    @PipelineStep(
//            order = 2,
//            inputType = String.class,
//            outputType = Integer.class,
//            stepType = io.github.mbarcia.pipeline.step.StepOneToMany.class,
//            backendType = GenericGrpcReactiveServiceAdapter.class,
//            grpcStub = TestGrpcStub.class,
//            grpcImpl = TestGrpcImpl.class,
//            inboundMapper = TestMapper.class,
//            outboundMapper = TestOutboundMapper.class,
//            grpcClient = "test-client-many",
//            autoPersist = true,
//            debug = true)
//    @ApplicationScoped
//    public static class TestOneToManyService implements ReactiveService<String, Integer> {
//        @Override
//        public Uni<Integer> process(String input) {
//            return Uni.createFrom().item(input.length());
//        }
//    }
//
//    // Local service test class - this represents a service that runs locally without
//    // gRPC
//    @PipelineStep(
//            order = 1,
//            inputType = String.class,
//            outputType = Integer.class,
//            stepType = io.github.mbarcia.pipeline.step.StepOneToMany.class,
//            backendType =
//                    GenericGrpcReactiveServiceAdapter.class, // This won't be used for local steps
//            // No gRPC stub for local steps
//            grpcImpl = Void.class, // No gRPC impl for local steps
//            inboundMapper = TestMapper.class,
//            outboundMapper = TestOutboundMapper.class,
//            // No gRPC client for local steps
//            local = true) // This is the key difference - local step
//    @ApplicationScoped
//    public static class TestLocalReactiveService implements ReactiveService<String, Integer> {
//        @Override
//        public Uni<Integer> process(String input) {
//            return Uni.createFrom().item(input.length());
//        }
//    }
//
//    // Supporting mock classes
//    public static class TestGrpcStub {}
//
//    public static class TestGrpcImpl {
//        // Placeholder gRPC implementation class
//    }
//
//    public static class TestMapper implements Mapper<String, String, String> {
//        @Override
//        public String fromGrpcFromDto(String grpcIn) {
//            return null;
//        }
//
//        @Override
//        public String toDtoToGrpc(String grpcInput) {
//            return grpcInput;
//        }
//
//        @Override
//        public String fromGrpc(String s) {
//            return null;
//        }
//
//        @Override
//        public String toGrpc(String s) {
//            return null;
//        }
//
//        @Override
//        public String fromDto(String s) {
//            return null;
//        }
//
//        @Override
//        public String toDto(String s) {
//            return null;
//        }
//    }
//
//    public static class TestOutboundMapper implements Mapper<Integer, Integer, Integer> {
//        @Override
//        public Integer fromGrpcFromDto(Integer grpcIn) {
//            return null;
//        }
//
//        @Override
//        public Integer toDtoToGrpc(Integer domainOutput) {
//            return domainOutput;
//        }
//
//        @Override
//        public Integer fromGrpc(Integer integer) {
//            return null;
//        }
//
//        @Override
//        public Integer toGrpc(Integer integer) {
//            return null;
//        }
//
//        @Override
//        public Integer fromDto(Integer integer) {
//            return null;
//        }
//
//        @Override
//        public Integer toDto(Integer integer) {
//            return null;
//        }
//    }
//
//    // Specific dummy implementations for testing
//    static class DummyAdditionalBeanBuildProducer
//            implements io.quarkus.deployment.annotations.BuildProducer<AdditionalBeanBuildItem> {
//        final java.util.List<AdditionalBeanBuildItem> items = new java.util.ArrayList<>();
//
//        @Override
//        public void produce(AdditionalBeanBuildItem item) {
//            items.add(item);
//        }
//    }
//
//    static class DummySyntheticBeanBuildProducer
//            implements io.quarkus.deployment.annotations.BuildProducer<
//                    io.quarkus.arc.deployment.SyntheticBeanBuildItem> {
//        final java.util.List<io.quarkus.arc.deployment.SyntheticBeanBuildItem> items =
//                new java.util.ArrayList<>();
//
//        @Override
//        public void produce(io.quarkus.arc.deployment.SyntheticBeanBuildItem item) {
//            items.add(item);
//        }
//    }
//
//    static class DummyGeneratedClassBuildProducer
//            implements io.quarkus.deployment.annotations.BuildProducer<
//                    io.quarkus.deployment.builditem.GeneratedClassBuildItem> {
//        final java.util.List<io.quarkus.deployment.builditem.GeneratedClassBuildItem> items =
//                new java.util.ArrayList<>();
//
//        @Override
//        public void produce(io.quarkus.deployment.builditem.GeneratedClassBuildItem item) {
//            items.add(item);
//        }
//    }
//
//    // Dummy implementation for UnremovableBeanBuildItem
//    static class DummyUnremovableBeanBuildProducer
//            implements io.quarkus.deployment.annotations.BuildProducer<
//                    io.quarkus.arc.deployment.UnremovableBeanBuildItem> {
//        final java.util.List<io.quarkus.arc.deployment.UnremovableBeanBuildItem> items =
//                new java.util.ArrayList<>();
//
//        @Override
//        public void produce(io.quarkus.arc.deployment.UnremovableBeanBuildItem item) {
//            items.add(item);
//        }
//    }
//
//    static class DummyGeneratedResourceBuildProducer
//            implements io.quarkus.deployment.annotations.BuildProducer<GeneratedResourceBuildItem>
// {
//        final List<GeneratedResourceBuildItem> items = new ArrayList<>();
//
//        @Override
//        public void produce(GeneratedResourceBuildItem item) {
//            items.add(item);
//        }
//    }
//
//    static class DummyReflectiveClassBuildItemBuildProducer
//            implements BuildProducer<ReflectiveClassBuildItem> {
//        private final List<ReflectiveClassBuildItem> items = new ArrayList<>();
//
//        @Override
//        public void produce(ReflectiveClassBuildItem item) {
//            items.add(item);
//        }
//    }
// }
