package com.example.poc;

import com.example.poc.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CsvFolder extends BaseEntity {
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "csvFolder",
            orphanRemoval = true
    )
    private final List<CsvPaymentsInputFile> files = new ArrayList<>();

    @NonNull
    private String folderPath;

    public CsvFolder(@NonNull String folderPath) {
        this.folderPath = folderPath;
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
