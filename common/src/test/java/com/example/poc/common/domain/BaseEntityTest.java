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

package com.example.poc.common.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

  // Test implementation of BaseEntity
  private static class TestEntity extends BaseEntity {}

  @Test
  void testConstructor() {
    // When
    TestEntity entity = new TestEntity();

    // Then
    assertNotNull(entity.getId());
    assertInstanceOf(UUID.class, entity.getId());
  }

  @Test
  void testGetId() {
    // Given
    TestEntity entity = new TestEntity();
    UUID id = entity.getId();

    // When
    UUID result = entity.getId();

    // Then
    assertEquals(id, result);
  }

  @Test
  void testSetId() {
    // Given
    TestEntity entity = new TestEntity();
    UUID newId = UUID.randomUUID();

    // When
    entity.setId(newId);

    // Then
    assertEquals(newId, entity.getId());
  }
}
