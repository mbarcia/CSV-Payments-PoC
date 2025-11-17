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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.Vertx;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReactiveServiceAdapterBaseTest {

    private static class TestAdapter extends ReactiveServiceAdapterBase {
        private boolean autoPersistEnabled;

        public TestAdapter(boolean autoPersistEnabled) {
            this.autoPersistEnabled = autoPersistEnabled;
        }

        @Override
        protected boolean isAutoPersistenceEnabled() {
            return autoPersistEnabled;
        }

        public Uni<Void> testSwitchToEventLoop() {
            return switchToEventLoop();
        }
    }

    @Test
    void testIsAutoPersistenceEnabledReturnsTrue() {
        // Given
        TestAdapter adapter = new TestAdapter(true);

        // When
        boolean result = adapter.isAutoPersistenceEnabled();

        // Then
        assertTrue(result, "Should return true when auto-persistence is enabled");
    }

    @Test
    void testIsAutoPersistenceEnabledReturnsFalse() {
        // Given
        TestAdapter adapter = new TestAdapter(false);

        // When
        boolean result = adapter.isAutoPersistenceEnabled();

        // Then
        assertFalse(result, "Should return false when auto-persistence is disabled");
    }

    @Test
    void testSwitchToEventLoopFailsWithoutVertxContext() {
        // Given
        TestAdapter adapter = new TestAdapter(true);

        // When - No Vert.x context available in plain JUnit test
        Uni<Void> result = adapter.testSwitchToEventLoop();

        // Then
        UniAssertSubscriber<Void> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(1)).getFailure();
        assertNotNull(failure, "Should fail without Vert.x context");
        assertTrue(failure instanceof IllegalStateException, 
            "Should throw IllegalStateException");
        assertTrue(failure.getMessage().contains("No Vert.x context available"), 
            "Error message should mention missing Vert.x context");
    }

    @Test
    void testSwitchToEventLoopReturnsUni() {
        // Given
        TestAdapter adapter = new TestAdapter(false);

        // When
        Uni<Void> result = adapter.testSwitchToEventLoop();

        // Then
        assertNotNull(result, "Should return a Uni instance");
    }

    @Test
    void testMultipleAdaptersWithDifferentSettings() {
        // Given
        TestAdapter adapter1 = new TestAdapter(true);
        TestAdapter adapter2 = new TestAdapter(false);

        // When/Then
        assertTrue(adapter1.isAutoPersistenceEnabled(), 
            "First adapter should have auto-persistence enabled");
        assertFalse(adapter2.isAutoPersistenceEnabled(), 
            "Second adapter should have auto-persistence disabled");
    }

    @Test
    void testAdapterCanBeExtended() {
        // Given/When - Verify that the class can be extended
        TestAdapter adapter = new TestAdapter(true);

        // Then
        assertNotNull(adapter, "Adapter should be instantiable");
        assertTrue(adapter instanceof ReactiveServiceAdapterBase, 
            "Should be instance of ReactiveServiceAdapterBase");
    }

    @Test
    void testSwitchToEventLoopBehaviorIsConsistent() {
        // Given
        TestAdapter adapter = new TestAdapter(true);

        // When - Call multiple times
        Uni<Void> result1 = adapter.testSwitchToEventLoop();
        Uni<Void> result2 = adapter.testSwitchToEventLoop();

        // Then - Both should fail consistently without Vert.x context
        UniAssertSubscriber<Void> subscriber1 = 
            result1.subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> subscriber2 = 
            result2.subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure1 = subscriber1.awaitFailure(Duration.ofSeconds(1)).getFailure();
        Throwable failure2 = subscriber2.awaitFailure(Duration.ofSeconds(1)).getFailure();

        assertNotNull(failure1);
        assertNotNull(failure2);
        assertEquals(failure1.getClass(), failure2.getClass(), 
            "Should fail with same exception type");
    }
}