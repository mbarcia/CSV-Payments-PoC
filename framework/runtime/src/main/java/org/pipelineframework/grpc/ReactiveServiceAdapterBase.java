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

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public abstract class ReactiveServiceAdapterBase {

  /**
   * Determines whether entities should be automatically persisted before processing. This method
   * should be implemented by generated service adapters to return the auto-persist value from
   * the @PipelineStep annotation.
   *
   * @return true if entities should be auto-persisted, false otherwise
   */
  protected abstract boolean isAutoPersistenceEnabled();

  /**
   * Switches execution to the current Vert.x event loop.
   *
   * If no Vert.x context is present the returned Uni fails with an IllegalStateException.
   *
   * @return a Uni that completes with `null` once the current Vert.x context has executed on its event loop,
   *         or fails with an {@link IllegalStateException} when no Vert.x context is available
   */
  protected Uni<Void> switchToEventLoop() {
    var ctx = Vertx.currentContext();
    if (ctx == null) {
      return Uni.createFrom().failure(new IllegalStateException("No Vert.x context available"));
    }
    return Uni.createFrom().emitter(em -> ctx.runOnContext(() -> em.complete(null)));
  }

  /**
   * Determines whether a Throwable represents a transient database connectivity issue.
   *
   * @param failure the throwable to inspect; its message will be checked for transient DB indicators
   * @return `true` if the throwable's message contains "connection refused", "connection closed" or "timeout", `false` otherwise
   */
  protected boolean isTransientDbError(Throwable failure) {
    String msg = failure.getMessage();
    return msg != null
        && (msg.contains("connection refused")
            || msg.contains("connection closed")
            || msg.contains("timeout"));
  }
}