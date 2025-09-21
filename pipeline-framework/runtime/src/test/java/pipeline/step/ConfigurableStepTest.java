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

package pipeline.step;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import org.junit.jupiter.api.Test;

class ConfigurableStepTest {

    static class TestStepBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {
        TestStepBlocking() {
            // No-args constructor
        }

        @Override
        public String apply(String input) {
            return "Processed: " + input;
        }
    }

    @Test
    void testConfigurableStepCreation() {
        // When
        TestStepBlocking step = new TestStepBlocking();

        // Then
        assertNotNull(step);
    }
}
