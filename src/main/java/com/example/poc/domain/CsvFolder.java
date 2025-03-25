package com.example.poc.domain;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Component
@Getter
@Setter
@NoArgsConstructor
public class CsvFolder {
    @Id
    @CsvIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        CsvFolder csvFolder = (CsvFolder) o;
//        return getFolderPath().equals(csvFolder.getFolderPath());
//    }
//
    @Override
    public int hashCode() {
        return Objects.hash(getFolderPath());
    }
}
