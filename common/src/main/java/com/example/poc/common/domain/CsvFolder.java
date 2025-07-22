package com.example.poc.common.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
public class CsvFolder extends BaseEntity {
    @Transient
    private final List<CsvPaymentsInputFile> files = new ArrayList<>();

    @NonNull
    private String folderPath;

    public CsvFolder(@NonNull String folderPath) {
        super();
        this.folderPath = folderPath;
    }

    public CsvFolder() {
        super();
    }

    public String toString() {
        return getFolderPath();
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
