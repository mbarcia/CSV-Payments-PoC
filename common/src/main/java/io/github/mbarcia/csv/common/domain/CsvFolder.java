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

package io.github.mbarcia.csv.common.domain;

import io.github.mbarcia.pipeline.domain.BaseEntity;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CsvFolder extends BaseEntity {
  @Transient private final List<CsvPaymentsInputFile> files = new ArrayList<>();

  @Convert(converter = PathConverter.class)
  private Path folderPath;

  public CsvFolder(Path folderPath) {
    super();
    this.folderPath = folderPath;
  }

  public CsvFolder() {
    super();
  }

  public String toString() {
    return getFolderPath().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CsvFolder csvFolder = (CsvFolder) o;
    return getFolderPath().equals(csvFolder.getFolderPath());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFolderPath());
  }
}
