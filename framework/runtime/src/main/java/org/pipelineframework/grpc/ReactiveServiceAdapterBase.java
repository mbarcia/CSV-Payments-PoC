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

/**
 * Base class for reactive service adapters that provide common functionality for gRPC reactive services.
 */
public abstract class ReactiveServiceAdapterBase {

    /**
     * Default constructor for ReactiveServiceAdapterBase.
     */

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

    // PostgreSQL-specific connection-related exceptions
    if (throwableClassName.equals("org.postgresql.util.PSQLException")) {
      // Check for SQL state codes that indicate connection issues
      // 08xxx = Connection Exception
      try {
        java.lang.reflect.Method getSQLStateMethod = throwable.getClass().getMethod("getSQLState");
        Object result = getSQLStateMethod.invoke(throwable);
        if (result != null) {
          String sqlState = result.toString();
          if (sqlState != null && sqlState.startsWith("08")) {
            return true;
          }
        }
      } catch (Exception e) {
        // If we can't access the SQL state through reflection, fall back to message inspection
        String message = throwable.getMessage();
        if (message != null) {
          String lowerMessage = message.toLowerCase();
          // Check for connection-related keywords in PostgreSQL exception messages
          if (lowerMessage.contains("connection refused") ||
              lowerMessage.contains("connection closed") ||
              lowerMessage.contains("connection lost") ||
              lowerMessage.contains("terminating connection") ||
              lowerMessage.contains("connection timeout")) {
            return true;
          }
        }
      }
      return false; // Only return true for actual connection-related PSQLExceptions
    }

    // MySQL-specific connection exceptions (more specific than just checking package name)
    if (throwableClassName.startsWith("com.mysql.cj.exceptions.")) {
      // Check for specific MySQL connection-related exception types
      if (throwableClassName.contains("CommunicationsException") ||
          throwableClassName.contains("ConnectionException") ||
          throwableClassName.contains("MySQLTimeoutException") ||
          throwableClassName.contains("SSLException")) {
        return true;
      }
      return false; // Only return true for specific connection-related MySQL exceptions
    }

    // Oracle-specific connection exceptions
    if (throwableClassName.startsWith("oracle.jdbc")) {
      // Check for Oracle connection-related exceptions
      if (throwableClassName.contains("OracleConnection") ||
          throwableClassName.contains("SQLRecoverableException")) {
        return true;
      }
      return false; // Only return true for connection-related Oracle exceptions
    }

    // Microsoft SQL Server exceptions
    if (throwableClassName.startsWith("com.microsoft.sqlserver.jdbc")) {
      // Check for SQL Server connection-related exceptions
      if (throwableClassName.contains("SQLServerException")) {
        // Check if the message indicates a connection issue
        String message = throwable.getMessage();
        if (message != null) {
          String lowerMessage = message.toLowerCase();
          // Common connection-related messages in SQL Server exceptions
          if (lowerMessage.contains("connection timed out") ||
              lowerMessage.contains("connection reset") ||
              lowerMessage.contains("the connection is closed") ||
              lowerMessage.contains("tcp provider") ||
              lowerMessage.contains("connection was terminated")) {
            return true;
          }
        }
      }
      return false; // Only return true for connection-related SQL Server exceptions
    }

    return false;
  }
}