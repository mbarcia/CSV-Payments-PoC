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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReactivePanachePersistenceProviderTest {

  private ReactivePanachePersistenceProvider provider;

  @BeforeEach
  void setUp() {
    provider = new ReactivePanachePersistenceProvider();
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
  void persist_WithNonPanacheEntity_ShouldReturnSameEntity() {
    Object entity = new Object();

    Uni<Object> resultUni = provider.persist(entity);

    UniAssertSubscriber<Object> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();

    assertSame(entity, subscriber.getItem());
  }

  @Test
  void supports_WithPanacheEntity_ShouldReturnTrue() {
    TestEntity entity = new TestEntity();

    boolean result = provider.supports(entity);

    assertTrue(result);
  }

  @Test
  void supports_WithNonPanacheEntity_ShouldReturnFalse() {
    Object entity = new Object();

    boolean result = provider.supports(entity);

    assertFalse(result);
  }

  @Test
  void supports_WithNullEntity_ShouldReturnFalse() {
    boolean result = provider.supports(null);

    assertFalse(result);
  }
}
