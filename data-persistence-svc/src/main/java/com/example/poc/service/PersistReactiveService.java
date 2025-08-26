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

package com.example.poc.service;

import com.example.poc.common.service.ReactiveService;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public interface PersistReactiveService<T> extends ReactiveService<T, T> {
    
    PersistReactiveRepository<T> getRepository();

    @Override
    default Uni<T> process(T processableObj) {
        Uni<T> uni = getRepository().persist(processableObj);

        String serviceId = this.getClass().toString();
        Logger logger = LoggerFactory.getLogger(this.getClass());
        MDC.put("serviceId", serviceId);
        logger.info("Persisted entity {}", processableObj);
        MDC.clear();

        return uni;
    }
}
