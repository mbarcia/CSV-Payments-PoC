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

package org.pipelineframework.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Multi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveBidirectionalStreamingService;

@ExtendWith(MockitoExtension.class)
class GrpcServiceBidirectionalStreamingAdapterTest {

    @Mock
    private ReactiveBidirectionalStreamingService<String, String> mockService;
    
    @Mock
    private PersistenceManager mockPersistenceManager;
    
    private TestBidirectionalAdapter adapter;
    
    class TestBidirectionalAdapter extends GrpcServiceBidirectionalStreamingAdapter<String, String, String, String> {
        
        @Override
        protected ReactiveBidirectionalStreamingService<String, String> getService() {
            return mockService;
        }

        @Override
        protected String fromGrpc(String grpcIn) {
            return "domain_" + grpcIn;
        }

        @Override
        protected String toGrpc(String domainOut) {
            return "grpc_" + domainOut;
        }
        
        @Override
        protected StepConfig getStepConfig() {
            return new StepConfig().autoPersist(true);
        }
    }
    
    @BeforeEach
    void setUp() {
        adapter = new TestBidirectionalAdapter();
        adapter.setPersistenceManager(mockPersistenceManager);
    }

    @Test
    void testRemoteProcessWithoutAutoPersistence() {
        // Disable auto-persistence
        adapter = new TestBidirectionalAdapter() {
            @Override
            protected StepConfig getStepConfig() {
                return new StepConfig().autoPersist(false);
            }
        };
        adapter.setPersistenceManager(mockPersistenceManager);
        
        // Prepare mock service to return a stream of results
        Multi<String> mockResult = Multi.createFrom().items("result1", "result2");
        when(mockService.process(any(Multi.class))).thenReturn(mockResult);
        
        // Create input stream
        Multi<String> input = Multi.createFrom().items("input1", "input2");
        
        // Execute
        Multi<String> result = adapter.remoteProcess(input);
        
        // Collect results
        List<String> results = new ArrayList<>();
        result.subscribe().with(results::add);
        
        // Verify results
        assertEquals(2, results.size());
        assertTrue(results.contains("grpc_result1"));
        assertTrue(results.contains("grpc_result2"));
        
        // Verify persistence was not called
        verify(mockPersistenceManager, never()).persist(any());
    }

    @Test
    void testRemoteProcessWithAutoPersistence() {
        // Prepare mock service to return a stream of results
        Multi<String> mockResult = Multi.createFrom().items("result1", "result2");
        when(mockService.process(any(Multi.class))).thenReturn(mockResult);
        
        // Mock persistence calls
        when(mockPersistenceManager.persist(any(String.class))).thenAnswer(invocation -> {
            String item = invocation.getArgument(0);
            return io.smallrye.mutiny.Uni.createFrom().item(item);
        });
        
        // Create input stream
        Multi<String> input = Multi.createFrom().items("input1", "input2");
        
        // Execute
        Multi<String> result = adapter.remoteProcess(input);
        
        // Collect results
        List<String> results = new ArrayList<>();
        result.subscribe().with(results::add);
        
        // Verify results
        assertEquals(2, results.size());
        assertTrue(results.contains("grpc_result1"));
        assertTrue(results.contains("grpc_result2"));
        
        // Verify persistence was called for each input after transformation
        verify(mockPersistenceManager, times(2)).persist(any(String.class));
    }

    @Test
    void testFromGrpcTransformation() {
        String grpcInput = "test";
        String domainInput = adapter.fromGrpc(grpcInput);
        
        assertEquals("domain_test", domainInput);
    }

    @Test
    void testToGrpcTransformation() {
        String domainOutput = "test";
        String grpcOutput = adapter.toGrpc(domainOutput);
        
        assertEquals("grpc_test", grpcOutput);
    }
}