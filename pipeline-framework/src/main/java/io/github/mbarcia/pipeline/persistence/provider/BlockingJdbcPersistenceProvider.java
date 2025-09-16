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

import io.github.mbarcia.pipeline.persistence.PersistenceProvider;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Blocking JDBC persistence provider using JPA EntityManager.
 * This implementation uses a dedicated thread pool to avoid blocking the reactive thread.
 */
@ApplicationScoped
public class BlockingJdbcPersistenceProvider implements PersistenceProvider<Object> {

    @PersistenceContext
    EntityManager entityManager;

    private final ExecutorService blockingExecutor = Executors.newCachedThreadPool();

    private final Map<Class<?>, Boolean> entityCache = new ConcurrentHashMap<>();

    @Override
    public Uni<Object> persist(Object entity) {
        if (entity == null || entityManager == null) {
            return Uni.createFrom().item(entity);
        }

        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                try {
                    entityManager.getTransaction().begin();
                    entityManager.persist(entity);
                    entityManager.getTransaction().commit();
                    return entity;
                } catch (Exception e) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    System.err.println("Failed to persist entity with JDBC: " + e.getMessage());
                    return entity;
                }
            }, blockingExecutor)
        );
    }

    @Override
    public boolean supports(Object entity) {
        // computeIfAbsent requires a Boolean return, auto-boxing handles it
        Class<?> clazz = entity.getClass();
        return entityCache.computeIfAbsent(clazz,
                c -> c.isAnnotationPresent(jakarta.persistence.Entity.class));
    }

    public void close() {
        if (blockingExecutor != null && !blockingExecutor.isShutdown()) {
            blockingExecutor.shutdown(); // stop accepting new tasks
            try {
                // wait a while for existing tasks to finish
                if (!blockingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    blockingExecutor.shutdownNow(); // cancel currently executing tasks
                    // wait again for tasks to respond to being cancelled
                    if (!blockingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("Executor did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                blockingExecutor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }
}