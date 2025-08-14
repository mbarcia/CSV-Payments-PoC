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

import static java.text.MessageFormat.format;

import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseCsvPaymentsFile extends BaseEntity implements Serializable {
  @Transient @NonNull protected File csvFile;

  @Convert(converter = PathConverter.class)
  protected Path filepath;

  @Transient private CsvFolder csvFolder;

  @Convert(converter = PathConverter.class)
  private Path csvFolderPath;

  public BaseCsvPaymentsFile(File file) {
    super();

    setCsvFile(file);
    setFilepath(Path.of(file.getPath()));
    setCsvFolderPath(Path.of(file.getParent()));
    setCsvFolder(new CsvFolder(getCsvFolderPath()));
  }

  @Override
  public String toString() {
    return format("CsvPaymentsFile'{'filepath=''{0}'''}'", filepath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFilepath());
  }
}
