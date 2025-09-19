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

package io.github.mbarcia.pipeline.service.impl;

import io.github.mbarcia.pipeline.domain.TestEntity;
import io.github.mbarcia.pipeline.domain.TestResult;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestReactiveService implements ReactiveService<TestEntity, TestResult> {
    
    @Override
    public Uni<TestResult> process(TestEntity entity) {
        // Simulate some processing
        String processedName = "Processed: " + entity.getName();
        String processedDescription = "Processed: " + entity.getDescription();
        
        TestResult result = new TestResult(processedName, processedDescription);
        
        return Uni.createFrom().item(result);
    }
}