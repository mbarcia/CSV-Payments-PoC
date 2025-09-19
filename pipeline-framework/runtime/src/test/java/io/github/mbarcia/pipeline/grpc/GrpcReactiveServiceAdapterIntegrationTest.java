/*
 * Copyright © 2023-2025 Mariano Barcia
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

package io.github.mbarcia.pipeline.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.domain.TestEntity;
import io.github.mbarcia.pipeline.domain.TestResult;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.service.impl.TestReactiveService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GrpcReactiveServiceAdapterIntegrationTest {

  @Mock private PersistenceManager mockPersistenceManager;

  private GrpcReactiveServiceAdapter<Object, Object, TestEntity, TestResult> adapter;

  private static class TestGrpcRequest {}

  private static class TestGrpcResponse {}

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Use reflection to inject the mock persistence manager
    adapter =
        new GrpcReactiveServiceAdapter<>() {
          @Override
          protected ReactiveService<TestEntity, TestResult> getService() {
            return new TestReactiveService();
          }

          @Override
          protected TestEntity fromGrpc(Object grpcIn) {
            return new TestEntity("test", "description");
          }

          @Override
          protected Object toGrpc(TestResult domainOut) {
            return new TestGrpcResponse();
          }

          @Override
          protected StepConfig getStepConfig() {
            return new StepConfig().autoPersist(true);
          }
        };

    try {
      java.lang.reflect.Field field =
          GrpcReactiveServiceAdapter.class.getDeclaredField("persistenceManager");
      field.setAccessible(true);
      field.set(adapter, mockPersistenceManager);
    } catch (Exception e) {
      fail("Failed to inject mock persistence manager: " + e.getMessage());
    }
  }

  @Test
  void remoteProcess_WithAutoPersistenceEnabled_ShouldPersistEntity() {
    TestGrpcRequest grpcRequest = new TestGrpcRequest();
    TestEntity entity = new TestEntity("test", "description");
    TestResult result = new TestResult("Processed: test", "Processed: description");

    // Mock the persistence manager to return the same entity
    when(mockPersistenceManager.persist(any(TestEntity.class)))
        .thenReturn(Uni.createFrom().item(entity));

    Uni<Object> resultUni = adapter.remoteProcess(grpcRequest);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertNotNull(subscriber.getItem());
    verify(mockPersistenceManager).persist(any(TestEntity.class));
  }

  @Test
  void remoteProcess_WithAutoPersistenceDisabled_ShouldNotPersistEntity() {
    GrpcReactiveServiceAdapter<Object, Object, TestEntity, TestResult> adapterWithoutPersistence =
        new GrpcReactiveServiceAdapter<>() {
          @Override
          protected ReactiveService<TestEntity, TestResult> getService() {
            return new TestReactiveService();
          }

          @Override
          protected TestEntity fromGrpc(Object grpcIn) {
            return new TestEntity("test", "description");
          }

          @Override
          protected Object toGrpc(TestResult domainOut) {
            return new TestGrpcResponse();
          }

          @Override
          protected StepConfig getStepConfig() {
            return new StepConfig().autoPersist(false);
          }
        };

    // Inject the mock persistence manager
    try {
      java.lang.reflect.Field field =
          GrpcReactiveServiceAdapter.class.getDeclaredField("persistenceManager");
      field.setAccessible(true);
      field.set(adapterWithoutPersistence, mockPersistenceManager);
    } catch (Exception e) {
      fail("Failed to inject mock persistence manager: " + e.getMessage());
    }

    TestGrpcRequest grpcRequest = new TestGrpcRequest();

    Uni<Object> resultUni = adapterWithoutPersistence.remoteProcess(grpcRequest);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertNotNull(subscriber.getItem());
    verify(mockPersistenceManager, never()).persist(any(TestEntity.class));
  }
}
