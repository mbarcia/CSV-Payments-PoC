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

package org.pipelineframework.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.enterprise.inject.Instance;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PersistenceManagerTest {

    @Mock Instance<PersistenceProvider<?>> mockProviderInstance;

    @Mock PersistenceProvider<?> mockProvider;

    private PersistenceManager persistenceManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        persistenceManager = new PersistenceManager();

        // Set up providerInstance mock
        when(mockProviderInstance.isUnsatisfied()).thenReturn(false);
        when(mockProviderInstance.stream()).thenReturn(Stream.of(mockProvider));

        // Inject it into the PersistenceManager
        try {
            Field field = PersistenceManager.class.getDeclaredField("providerInstance");
            field.setAccessible(true);
            field.set(persistenceManager, mockProviderInstance);
        } catch (Exception e) {
            fail("Failed to inject mock providerInstance: " + e.getMessage());
        }

        // Manually call PostConstruct
        persistenceManager.init();
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
        // Reset the mock to return no providers
        when(mockProviderInstance.isUnsatisfied()).thenReturn(true);
        when(mockProviderInstance.stream()).thenReturn(java.util.stream.Stream.empty());

        // Re-initialize the provider list in the PersistenceManager
        reinitializeProviders();

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
    }

    @Test
    void persist_WithSupportedProvider_ShouldUseProvider() {
        Object entity = new Object();
        PersistenceProvider<Object> specificMockProvider = mock(PersistenceProvider.class);

        // Set up the mock instance to use our specific provider
        when(mockProviderInstance.isUnsatisfied()).thenReturn(false);
        when(mockProviderInstance.stream())
                .thenReturn(java.util.stream.Stream.of(specificMockProvider));
        when(specificMockProvider.supports(entity)).thenReturn(true);
        when(specificMockProvider.persist(entity)).thenReturn(Uni.createFrom().item(entity));

        // Re-initialize the provider list in the PersistenceManager
        reinitializeProviders();

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
        verify(specificMockProvider).supports(entity);
        verify(specificMockProvider).persist(entity);
    }

    @Test
    void persist_WithUnsupportedProvider_ShouldReturnSameEntity() {
        Object entity = new Object();
        PersistenceProvider<Object> specificMockProvider = mock(PersistenceProvider.class);

        when(mockProviderInstance.isUnsatisfied()).thenReturn(false);
        when(mockProviderInstance.stream())
                .thenReturn(java.util.stream.Stream.of(specificMockProvider));
        when(specificMockProvider.supports(entity)).thenReturn(false);

        // Re-initialize the provider list in the PersistenceManager
        reinitializeProviders();

        Uni<Object> resultUni = persistenceManager.persist(entity);

        UniAssertSubscriber<Object> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();

        assertSame(entity, subscriber.getItem());
        verify(specificMockProvider).supports(entity);
        verify(specificMockProvider, never()).persist(any());
    }

    /** Helper method to reinitialize the providers list after changing mock configuration */
    private void reinitializeProviders() {
        try {
            Field providersField = PersistenceManager.class.getDeclaredField("providers");
            providersField.setAccessible(true);
            List<PersistenceProvider<?>> updatedProviders = mockProviderInstance.stream().toList();
            providersField.set(persistenceManager, updatedProviders);
        } catch (Exception e) {
            fail("Failed to reinitialize providers list: " + e.getMessage());
        }
    }
}
