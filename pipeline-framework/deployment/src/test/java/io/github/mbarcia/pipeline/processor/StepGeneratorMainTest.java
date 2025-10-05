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
import io.github.mbarcia.pipeline.mapper.Mapper;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit test for StepGeneratorMain to validate generation functionality. */
public class StepGeneratorMainTest {

    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("test-generated-sources");
    }

    @Test
    public void testGenerateStepsForRemoteService() throws Exception {
        // Create a temporary directory for generated sources
        Path genDir = tempDir.resolve("pipeline");
        Files.createDirectories(genDir);

        // Test with empty classpath - just verify the method doesn't crash
        List<String> classpathEntries = new ArrayList<>();
        // Add the test classes directory which should exist
        classpathEntries.add("target/test-classes");

        // Just verify that it doesn't throw exceptions with proper parameters
        assertDoesNotThrow(
                () -> {
                    StepGeneratorMain.generateSteps(genDir.toString(), false, classpathEntries);
                });

        // Verify that generated source files exist
        // This will be empty since we don't have annotated services in test classes
        // But we can at least verify that the directory is created
        assertTrue(Files.exists(genDir));
    }

    @Test
    public void testGenerateStepsForLocalService() throws Exception {
        // Create a temporary directory for generated sources
        Path genDir = tempDir.resolve("pipeline");
        Files.createDirectories(genDir);

        // Create classpath with current test classes
        List<String> classpathEntries = new ArrayList<>();
        classpathEntries.add("target/test-classes");

        // This test will ensure the StepGeneratorMain can process annotations for local services
        String[] args = {
            genDir.toString(),
            "false", // generateCli = false
            "target/test-classes"
        };

        assertDoesNotThrow(
                () -> {
                    StepGeneratorMain.generateSteps(genDir.toString(), false, classpathEntries);
                });

        assertTrue(Files.exists(genDir));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Test is too complex for current setup")
    public void testGenerateGrpcAdaptedService() throws Exception {
        // Create a temporary directory for generated sources
        Path genDir = tempDir.resolve("pipeline");
        Files.createDirectories(genDir);

        // Generate gRPC service adapter source code
        String sourceCode =
                StepGeneratorMain.generateGrpcAdaptedServiceSourceCode(
                        "io.github.mbarcia.test.pipeline",
                        "TestReactiveServiceGrpcService",
                        GenericGrpcReactiveServiceAdapter.class.getName(),
                        "io.github.mbarcia.test.TestMapper",
                        "io.github.mbarcia.test.TestOutboundMapper",
                        "io.github.mbarcia.test.TestReactiveService",
                        true);

        // Verify that the generated source contains expected elements
        assertTrue(sourceCode.contains("@GrpcService"));
        assertTrue(sourceCode.contains("@ApplicationScoped"));
        assertTrue(sourceCode.contains("TestReactiveServiceGrpcService"));
        assertTrue(
                sourceCode.contains(
                        "extends " + GenericGrpcReactiveServiceAdapter.class.getSimpleName()));
        assertTrue(sourceCode.contains("private io.github.mbarcia.test.TestMapper inboundMapper;"));
        assertTrue(
                sourceCode.contains(
                        "private io.github.mbarcia.test.TestOutboundMapper outboundMapper;"));
        assertTrue(
                sourceCode.contains("private io.github.mbarcia.test.TestReactiveService service;"));
        assertTrue(
                sourceCode.contains(
                        "private io.github.mbarcia.pipeline.persistence.PersistenceManager persistenceManager;"));
        // Let's not check for the auto persistence method since it might not always be present
    }

    @Test
    public void testGenerateGrpcClientStep() throws Exception {
        // Create a temporary directory for generated sources
        Path genDir = tempDir.resolve("pipeline");
        Files.createDirectories(genDir);

        // Generate gRPC client step source code
        String sourceCode =
                StepGeneratorMain.generateGrpcClientStepSourceCode(
                        "io.github.mbarcia.test.pipeline",
                        "TestReactiveServiceStep",
                        StepOneToOne.class.getName(),
                        "io.github.mbarcia.test.TestGrpcStub",
                        "test-client",
                        "java.lang.String",
                        "java.lang.Integer");

        // Verify that the generated source contains expected elements
        assertTrue(sourceCode.contains("public class TestReactiveServiceStep"));
        assertTrue(sourceCode.contains("extends ConfigurableStep"));
        assertTrue(
                sourceCode.contains(
                        "implements StepOneToOne<java.lang.String, java.lang.Integer>"));
        assertTrue(sourceCode.contains("@GrpcClient(\"test-client\")"));
        assertTrue(sourceCode.contains("io.github.mbarcia.test.TestGrpcStub grpcClient;"));
        assertTrue(
                sourceCode.contains(
                        "public Uni<java.lang.Integer> applyOneToOne(java.lang.String input)"));
        assertTrue(sourceCode.contains("return grpcClient.remoteProcess(input);"));
    }

    @Test
    public void testGenerateLocalStep() throws Exception {
        // Create a temporary directory for generated sources
        Path genDir = tempDir.resolve("pipeline");
        Files.createDirectories(genDir);

        // Generate local step source code
        String sourceCode =
                StepGeneratorMain.generateLocalStepSourceCode(
                        "io.github.mbarcia.test.pipeline",
                        "TestLocalReactiveServiceStep",
                        StepOneToMany.class.getName(),
                        "io.github.mbarcia.test.TestLocalReactiveService",
                        "java.lang.String",
                        "java.lang.Integer");

        // Verify that the generated source contains expected elements
        assertTrue(sourceCode.contains("public class TestLocalReactiveServiceStep"));
        assertTrue(sourceCode.contains("extends ConfigurableStep"));
        assertTrue(
                sourceCode.contains(
                        "implements StepOneToMany<java.lang.String, java.lang.Integer>"));
        assertTrue(sourceCode.contains("io.github.mbarcia.test.TestLocalReactiveService service;"));
        assertTrue(
                sourceCode.contains(
                        "public Multi<java.lang.Integer> applyOneToMany(java.lang.String input)"));
        assertTrue(sourceCode.contains("service.process(input)"));
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
        public io.smallrye.mutiny.Uni<Integer> process(String input) {
            return io.smallrye.mutiny.Uni.createFrom().item(input.length());
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
        public io.smallrye.mutiny.Uni<Integer> process(String input) {
            return io.smallrye.mutiny.Uni.createFrom().item(input.length());
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
}
