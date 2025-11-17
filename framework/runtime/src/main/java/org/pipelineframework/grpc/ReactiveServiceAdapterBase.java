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
   * @param failure the throwable to inspect; walks the cause chain checking each message and type for transient DB indicators
   * @return `true` if any exception in the cause chain has a message containing "connection refused", "connection closed",
   * "timeout", "connection reset", "communications link failure" (case-insensitive) or is of a known transient exception type,
   * `false` otherwise
   */
  protected boolean isTransientDbError(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      // Check if the current exception is of a known transient type
      if (isKnownTransientExceptionType(current)) {
        return true;
      }

      // Check for transient indicators in the message (case-insensitive)
      String msg = current.getMessage();
      if (msg != null) {
        String lowerMsg = msg.toLowerCase();
        if (lowerMsg.contains("connection refused")
            || lowerMsg.contains("connection closed")
            || lowerMsg.contains("timeout")
            || lowerMsg.contains("connection reset")
            || lowerMsg.contains("communications link failure")) {
          return true;
        }
      }

      // Move to the cause
      current = current.getCause();

      // Prevent infinite loops if there's a circular cause
      if (current == failure) {
        break;
      }
    }

    return false;
  }

  /**
   * Determines if the given exception is of a type that indicates a transient database error.
   *
   * @param throwable the exception to check
   * @return true if the exception type is known to indicate transient database errors
   */
  private boolean isKnownTransientExceptionType(Throwable throwable) {
    // SQL transient exceptions
    if (throwable instanceof java.sql.SQLTransientException) {
      return true;
    }

    // Hibernate Reactive specific transient exceptions (if they exist)
    // Check for common Hibernate and database driver transient exceptions
    String throwableClassName = throwable.getClass().getName();
    if (throwableClassName.contains("hibernate") &&
        (throwableClassName.toLowerCase().contains("transient") ||
         throwableClassName.toLowerCase().contains("connection") ||
         throwableClassName.toLowerCase().contains("timeout"))) {
      return true;
    }

    // Additional database driver specific transient exceptions
    if (throwableClassName.contains("org.postgresql.util.PSQLException") ||
        throwableClassName.contains("com.mysql.cj.exceptions") ||
        throwableClassName.contains("oracle") ||
        throwableClassName.contains("sqlserver")) {
      // For specific database exceptions that might be transient
      return true;
    }

    return false;
  }
}