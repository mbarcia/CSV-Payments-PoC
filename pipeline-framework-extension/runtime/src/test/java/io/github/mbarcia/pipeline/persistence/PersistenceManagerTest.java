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

package io.github.mbarcia.pipeline.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PersistenceManagerTest {

    @Mock Instance<PersistenceProvider<?>> mockProviders;

    private PersistenceManager persistenceManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        persistenceManager = new PersistenceManager();
        // Use reflection to inject the mock providers
        try {
            java.lang.reflect.Field field = PersistenceManager.class.getDeclaredField("providers");
            field.setAccessible(true);
            field.set(persistenceManager, mockProviders);
        } catch (Exception e) {
            fail("Failed to inject mock providers: " + e.getMessage());
        }
    }

    @Test
    void persist_WithNullEntity_ShouldReturnSameEntity() {
        Object entity = null;

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertNull(subscriber.getItem());
    }

    @Test
    void persist_WithNoProviders_ShouldReturnSameEntity() {
        Object entity = new Object();
        when(mockProviders.isUnsatisfied()).thenReturn(true);

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
        verify(mockProviders).isUnsatisfied();
    }

    @Test
    void persist_WithSupportedProvider_ShouldUseProvider() {
        Object entity = new Object();
        PersistenceProvider<Object> mockProvider = mock(PersistenceProvider.class);

        when(mockProviders.isUnsatisfied()).thenReturn(false);
        when(mockProviders.stream()).thenReturn(java.util.stream.Stream.of(mockProvider));
        when(mockProvider.supports(entity)).thenReturn(true);
        when(mockProvider.persist(entity)).thenReturn(Uni.createFrom().item(entity));

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
        verify(mockProvider).supports(entity);
        verify(mockProvider).persist(entity);
    }

    @Test
    void persist_WithUnsupportedProvider_ShouldReturnSameEntity() {
        Object entity = new Object();
        PersistenceProvider<Object> mockProvider = mock(PersistenceProvider.class);

        when(mockProviders.isUnsatisfied()).thenReturn(false);
        when(mockProviders.stream()).thenReturn(java.util.stream.Stream.of(mockProvider));
        when(mockProvider.supports(entity)).thenReturn(false);

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
        verify(mockProvider).supports(entity);
        verify(mockProvider, never()).persist(any());
    }
}
