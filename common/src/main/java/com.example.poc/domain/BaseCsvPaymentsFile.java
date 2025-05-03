package com.example.poc.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import static java.text.MessageFormat.format;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseCsvPaymentsFile extends BaseEntity implements Serializable {
    @Transient
    @NonNull
    protected File csvFile;
    protected String filepath;

    @Transient
    private CsvFolder csvFolder;
    private UUID csvFolderId;

    @Override
    public String toString() {
        return format("CsvPaymentsFile'{'filepath=''{0}'''}'", filepath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilepath());
    }
}
