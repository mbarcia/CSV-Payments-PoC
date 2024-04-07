package com.example.poc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
public class CsvFolder extends BasePersistable {
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "csvFolder",
            orphanRemoval = true
    )
    private final List<CsvPaymentsFile> files = new ArrayList<>();

    @NonNull
    private String folderPath;

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
