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

package io.github.mbarcia.csv.service;

import io.github.mbarcia.csv.CsvPaymentsApplication;
import io.quarkus.runtime.Quarkus;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class CsvPaymentsApplicationTest {

  @Test
  void testMain() {
    // Arrange
    String[] args = {"--csv-folder", "test-folder"};
    try (MockedStatic<Quarkus> quarkusMock = Mockito.mockStatic(Quarkus.class)) {
      // Act
      CsvPaymentsApplication.main(args);

      // Assert
      quarkusMock.verify(() -> Quarkus.run(CsvPaymentsApplication.class, args));
    }
  }
}
