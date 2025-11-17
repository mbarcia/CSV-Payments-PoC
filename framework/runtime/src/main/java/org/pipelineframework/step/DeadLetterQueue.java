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

package org.pipelineframework.step;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

public interface DeadLetterQueue<I, O> {

    Logger LOG = Logger.getLogger(DeadLetterQueue.class);

    default Uni<O> deadLetter(Uni<I> failedItem, Throwable cause) {
        LOG.errorf("DLQ drop: item=%s cause=%s", failedItem.toString(), cause);
        return io.smallrye.mutiny.Uni.createFrom().nullItem();
    }
}
