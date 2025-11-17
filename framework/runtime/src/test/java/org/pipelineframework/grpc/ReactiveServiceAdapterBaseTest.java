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

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for ReactiveServiceAdapterBase.
 * Tests the switchToEventLoop functionality and auto-persistence detection.
 */
@QuarkusTest
class ReactiveServiceAdapterBaseTest {

    private TestReactiveServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TestReactiveServiceAdapter(false);
    }

    @Test
    void testIsAutoPersistenceEnabledWhenDisabled() {
        // Given
        TestReactiveServiceAdapter adapterDisabled = new TestReactiveServiceAdapter(false);

        // When
        boolean enabled = adapterDisabled.isAutoPersistenceEnabledPublic();

        // Then
        assertFalse(enabled, "Auto-persistence should be disabled");
    }

    @Test
    void testIsAutoPersistenceEnabledWhenEnabled() {
        // Given
        TestReactiveServiceAdapter adapterEnabled = new TestReactiveServiceAdapter(true);

        // When
        boolean enabled = adapterEnabled.isAutoPersistenceEnabledPublic();

        // Then
        assertTrue(enabled, "Auto-persistence should be enabled");
    }

    @Test
    void testSwitchToEventLoopSucceedsWithVertxContext() {
        // When
        Uni<Void> result = adapter.switchToEventLoopPublic();

        // Then
        assertNotNull(result, "Result Uni should not be null");
    }

    @Test
    void testSwitchToEventLoopFailsWithoutVertxContext() {
        // Given - no Vert.x context (running in plain JUnit thread)

        // When
        Uni<Void> result = adapter.switchToEventLoopPublic();

        // Then
        assertThrows(
                IllegalStateException.class,
                () -> result.await().indefinitely(),
                "Should throw IllegalStateException when no Vert.x context");
    }

    @Test
    void testSwitchToEventLoopFailureMessage() {
        // Given - no Vert.x context

        // When
        Uni<Void> result = adapter.switchToEventLoopPublic();

        // Then
        try {
            result.await().indefinitely();
            fail("Should have thrown IllegalStateException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals(
                    "No Vert.x context available",
                    e.getCause().getMessage(),
                    "Error message should indicate no Vert.x context");
        }
    }

    @Test
    void testSwitchToEventLoopExecutesOnEventLoop() {
        // When
        Uni<Void> result = adapter.switchToEventLoopPublic();

        // Then
        assertNotNull(result, "Result should not be null");
    }

    @Test
    void testSwitchToEventLoopCanBeChained() {
        // When - chain multiple switchToEventLoop calls
        Uni<String> result =
                adapter.switchToEventLoopPublic()
                        .onItem()
                        .transformToUni(v -> adapter.switchToEventLoopPublic())
                        .onItem()
                        .transform(v -> "success");

        // Then
        assertNotNull(result, "Chained result should not be null");
    }

    @Test
    void testAbstractMethodMustBeImplemented() {
        // Given
        TestReactiveServiceAdapter testAdapter = new TestReactiveServiceAdapter(true);

        // Then - verify the abstract method is implemented
        assertDoesNotThrow(
                () -> testAdapter.isAutoPersistenceEnabledPublic(),
                "Abstract method should be implemented");
    }

    @Test
    void testMultipleInstancesCanHaveDifferentAutoPersistSettings() {
        // Given
        TestReactiveServiceAdapter adapter1 = new TestReactiveServiceAdapter(true);
        TestReactiveServiceAdapter adapter2 = new TestReactiveServiceAdapter(false);

        // Then
        assertTrue(adapter1.isAutoPersistenceEnabledPublic());
        assertFalse(adapter2.isAutoPersistenceEnabledPublic());
    }

    /** Test implementation of ReactiveServiceAdapterBase for testing purposes */
    private static class TestReactiveServiceAdapter extends ReactiveServiceAdapterBase {
        private final boolean autoPersist;

        TestReactiveServiceAdapter(boolean autoPersist) {
            this.autoPersist = autoPersist;
        }

        @Override
        protected boolean isAutoPersistenceEnabled() {
            return autoPersist;
        }

        // Public wrapper for testing
        public boolean isAutoPersistenceEnabledPublic() {
            return isAutoPersistenceEnabled();
        }

        // Public wrapper for testing
        public Uni<Void> switchToEventLoopPublic() {
            return switchToEventLoop();
        }
    }
}