package com.example.poc.common.domain;

import static java.text.MessageFormat.format;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseCsvPaymentsFile extends BaseEntity implements Serializable {
  @Transient @NonNull protected File csvFile;

  protected String filepath;

  @Transient private CsvFolder csvFolder;

  private String csvFolderPath;

  public BaseCsvPaymentsFile() {
    super();
  }

  protected void assignFileAndFolder(File file) {
    setCsvFile(file);
    setFilepath(file.getPath());
    setCsvFolderPath(file.getParent());
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
