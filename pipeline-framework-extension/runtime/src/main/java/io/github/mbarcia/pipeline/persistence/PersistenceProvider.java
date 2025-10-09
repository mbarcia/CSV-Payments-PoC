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

import io.smallrye.mutiny.Uni;

/**
 * Abstraction for persistence operations that can work with different database technologies.
 * 
 * @param <T> The type of entity to persist
 */
public interface PersistenceProvider<T> {
    
    /**
     * Persist an entity and return a Uni that completes when the operation is done.
     * 
     * @param entity The entity to persist
     * @return A Uni that completes with the persisted entity
     */
    Uni<T> persist(T entity);
    
    /**
     * Check if this provider can handle the given entity type.
     * 
     * @param entity The entity to check
     * @return true if this provider can handle the entity, false otherwise
     */
    boolean supports(Object entity);
}