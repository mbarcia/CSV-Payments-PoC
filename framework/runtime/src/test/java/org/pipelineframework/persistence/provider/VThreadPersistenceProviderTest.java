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

package org.pipelineframework.persistence.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class VThreadPersistenceProviderTest {

    private VThreadPersistenceProvider provider;

    @Entity
    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class NonEntityClass {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @BeforeEach
    void setUp() {
        // Note: This provider requires Arc container context which may not be available in unit tests
        // These tests focus on the logical behavior
        provider = new VThreadPersistenceProvider();
    }

    @Test
    void testTypeReturnsObjectClass() {
        // Given/When
        Class<Object> type = provider.type();

        // Then
        assertNotNull(type, "Type should not be null");
        assertEquals(Object.class, type, "Type should be Object.class");
    }

    @Test
    void testSupportsEntityAnnotatedClass() {
        // Given
        TestEntity entity = new TestEntity();

        // When
        boolean supports = provider.supports(entity);

        // Then
        assertTrue(supports, "Should support entity annotated with @Entity");
    }

    @Test
    void testDoesNotSupportNonEntityClass() {
        // Given
        NonEntityClass nonEntity = new NonEntityClass();

        // When
        boolean supports = provider.supports(nonEntity);

        // Then
        assertFalse(supports, "Should not support class without @Entity annotation");
    }

    @Test
    void testSupportsThreadContextReturnsTrueForVirtualThreads() {
        // Given/When
        boolean supportsThreadContext = provider.supportsThreadContext();

        // Then
        // This will be true only if running on a virtual thread
        // In regular test execution, it will be false
        assertEquals(Thread.currentThread().isVirtual(), supportsThreadContext, 
            "Should match current thread's virtual status");
    }

    @Test
    void testSupportsThreadContextReturnsFalseForPlatformThreads() {
        // Given/When
        // Running in a standard JUnit test (platform thread)
        boolean supportsThreadContext = provider.supportsThreadContext();

        // Then
        if (!Thread.currentThread().isVirtual()) {
            assertFalse(supportsThreadContext, 
                "Should return false for platform threads");
        }
    }

    @Test
    void testPersistWithMockedEntityManager() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setName("test");

        EntityManager mockEm = mock(EntityManager.class);
        EntityTransaction mockTransaction = mock(EntityTransaction.class);

        when(mockEm.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.isActive()).thenReturn(false);

        @SuppressWarnings("unchecked")
        InjectableInstance<EntityManager> mockInstance = mock(InjectableInstance.class);
        when(mockInstance.isResolvable()).thenReturn(true);
        when(mockInstance.get()).thenReturn(mockEm);

        try (MockedStatic<Arc> arcMock = mockStatic(Arc.class)) {
            io.quarkus.arc.ArcContainer mockContainer = mock(io.quarkus.arc.ArcContainer.class);
            arcMock.when(Arc::container).thenReturn(mockContainer);
            when(mockContainer.select(EntityManager.class)).thenReturn(mockInstance);

            VThreadPersistenceProvider testProvider = new VThreadPersistenceProvider();

            // When
            Uni<Object> result = testProvider.persist(entity);

            // Then
            UniAssertSubscriber<Object> subscriber = 
                result.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.awaitItem();

            assertSame(entity, subscriber.getItem(), "Should return the same entity instance");
            verify(mockTransaction).begin();
            verify(mockEm).persist(entity);
            verify(mockTransaction).commit();
        }
    }

    @Test
    void testPersistRollsBackOnException() {
        // Given
        TestEntity entity = new TestEntity();

        EntityManager mockEm = mock(EntityManager.class);
        EntityTransaction mockTransaction = mock(EntityTransaction.class);

        when(mockEm.getTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.isActive()).thenReturn(true);
        doThrow(new RuntimeException("Persist failed")).when(mockEm).persist(entity);

        @SuppressWarnings("unchecked")
        InjectableInstance<EntityManager> mockInstance = mock(InjectableInstance.class);
        when(mockInstance.isResolvable()).thenReturn(true);
        when(mockInstance.get()).thenReturn(mockEm);

        try (MockedStatic<Arc> arcMock = mockStatic(Arc.class)) {
            io.quarkus.arc.ArcContainer mockContainer = mock(io.quarkus.arc.ArcContainer.class);
            arcMock.when(Arc::container).thenReturn(mockContainer);
            when(mockContainer.select(EntityManager.class)).thenReturn(mockInstance);

            VThreadPersistenceProvider testProvider = new VThreadPersistenceProvider();

            // When
            Uni<Object> result = testProvider.persist(entity);

            // Then
            UniAssertSubscriber<Object> subscriber = 
                result.subscribe().withSubscriber(UniAssertSubscriber.create());
            
            assertThrows(RuntimeException.class, subscriber::awaitItem, 
                "Should propagate exception from persist");
            verify(mockTransaction).begin();
            verify(mockTransaction).rollback();
            verify(mockTransaction, never()).commit();
        }
    }

    @Test
    void testPersistFailsWhenNoEntityManagerAvailable() {
        // Given
        TestEntity entity = new TestEntity();

        @SuppressWarnings("unchecked")
        InjectableInstance<EntityManager> mockInstance = mock(InjectableInstance.class);
        when(mockInstance.isResolvable()).thenReturn(false);

        try (MockedStatic<Arc> arcMock = mockStatic(Arc.class)) {
            io.quarkus.arc.ArcContainer mockContainer = mock(io.quarkus.arc.ArcContainer.class);
            arcMock.when(Arc::container).thenReturn(mockContainer);
            when(mockContainer.select(EntityManager.class)).thenReturn(mockInstance);

            VThreadPersistenceProvider testProvider = new VThreadPersistenceProvider();

            // When
            Uni<Object> result = testProvider.persist(entity);

            // Then
            UniAssertSubscriber<Object> subscriber = 
                result.subscribe().withSubscriber(UniAssertSubscriber.create());
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                subscriber::awaitItem, 
                "Should throw IllegalStateException when EntityManager is not available");
            assertTrue(exception.getMessage().contains("No EntityManager available"), 
                "Exception message should indicate missing EntityManager");
        }
    }

    @Test
    void testSupportsHandlesNullGracefully() {
        // Given
        Object nullEntity = null;

        // When/Then
        assertThrows(NullPointerException.class, () -> provider.supports(nullEntity), 
            "Should handle null entity appropriately");
    }
}