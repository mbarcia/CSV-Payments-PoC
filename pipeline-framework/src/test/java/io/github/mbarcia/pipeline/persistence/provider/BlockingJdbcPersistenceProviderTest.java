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

package io.github.mbarcia.pipeline.persistence.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mbarcia.pipeline.domain.TestEntity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockingJdbcPersistenceProviderTest {

  private BlockingJdbcPersistenceProvider provider;
  private EntityManager mockEntityManager;
  private EntityTransaction mockTransaction;

  @BeforeEach
  void setUp() {
    provider = new BlockingJdbcPersistenceProvider();
    mockEntityManager = mock(EntityManager.class);
    mockTransaction = mock(EntityTransaction.class);

    // Use reflection to inject the mock entity manager
    try {
      java.lang.reflect.Field field =
          BlockingJdbcPersistenceProvider.class.getDeclaredField("entityManager");
      field.setAccessible(true);
      field.set(provider, mockEntityManager);
    } catch (Exception e) {
      fail("Failed to inject mock entity manager: " + e.getMessage());
    }
  }

  @Test
  void persist_WithNullEntity_ShouldReturnSameEntity() {
    Object entity = null;

    Uni<Object> resultUni = provider.persist(entity);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertNull(subscriber.getItem());
  }

  @Test
  void persist_WithNullEntityManager_ShouldReturnSameEntity() {
    // Set entityManager to null
    try {
      java.lang.reflect.Field field =
          BlockingJdbcPersistenceProvider.class.getDeclaredField("entityManager");
      field.setAccessible(true);
      field.set(provider, null);
    } catch (Exception e) {
      fail("Failed to set entity manager to null: " + e.getMessage());
    }

    Object entity = new Object();

    Uni<Object> resultUni = provider.persist(entity);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertSame(entity, subscriber.getItem());
  }

  @Test
  void persist_WithEntityManager_ShouldPersistEntity() {
    when(mockEntityManager.getTransaction()).thenReturn(mockTransaction);
    when(mockTransaction.isActive()).thenReturn(false);

    TestEntity entity = new TestEntity("test", "description");

    Uni<Object> resultUni = provider.persist(entity);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertSame(entity, subscriber.getItem());
    verify(mockEntityManager).getTransaction();
    verify(mockEntityManager).persist(entity);
    verify(mockTransaction).begin();
    verify(mockTransaction).commit();
  }

  @Test
  void supports_WithEntityManager_ShouldReturnTrue() {
    TestEntity entity = new TestEntity();

    boolean result = provider.supports(entity);

    assertTrue(result);
  }

  @Test
  void supports_WithNullEntityManager_ShouldReturnFalse() {
    // Set entityManager to null
    try {
      java.lang.reflect.Field field =
          BlockingJdbcPersistenceProvider.class.getDeclaredField("entityManager");
      field.setAccessible(true);
      field.set(provider, null);
    } catch (Exception e) {
      fail("Failed to set entity manager to null: " + e.getMessage());
    }

    TestEntity entity = new TestEntity();

    boolean result = provider.supports(entity);

    assertFalse(result);
  }

  @Test
  void supports_WithNullEntity_ShouldReturnFalse() {
    boolean result = provider.supports(null);

    assertFalse(result);
  }
}
