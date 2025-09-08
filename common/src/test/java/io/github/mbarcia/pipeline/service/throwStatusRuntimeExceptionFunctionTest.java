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

package io.github.mbarcia.pipeline.service;

import static org.junit.jupiter.api.Assertions.*;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

class ThrowStatusRuntimeExceptionFunctionTest {

  @Test
  void apply_ShouldWrapThrowableInStatusRuntimeException() {
    // Given
    throwStatusRuntimeExceptionFunction function = new throwStatusRuntimeExceptionFunction();
    Throwable originalThrowable = new RuntimeException("Original error message");

    // When
    Throwable result = function.apply(originalThrowable);

    // Then
    assertNotNull(result);
    assertInstanceOf(StatusRuntimeException.class, result);

    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) result;
    assertEquals(Status.INTERNAL.getCode(), statusRuntimeException.getStatus().getCode());
    assertEquals(
        originalThrowable.getMessage(), statusRuntimeException.getStatus().getDescription());
    assertSame(originalThrowable, statusRuntimeException.getStatus().getCause());

    Metadata metadata = statusRuntimeException.getTrailers();
    assertNotNull(metadata);
    assertEquals(
        originalThrowable.getMessage(),
        metadata.get(Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER)));
  }
}
