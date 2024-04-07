package com.example.poc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class CsvPaymentsFile extends BasePersistable implements AutoCloseable {
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "csvPaymentsFile",
            orphanRemoval = true
    )
    private final List<PaymentRecord> records = new ArrayList<>();

    @Transient
    @NonNull
    private File csvFile;

    @ManyToOne(fetch = FetchType.LAZY)
    private CsvFolder csvFolder;

    private String filepath;

    public CsvPaymentsFile(@NonNull File csvFile) {
        this.csvFile = csvFile;
        this.filepath = csvFile.getPath();
    }

    @Override
    public String toString() {
        return "CsvPaymentsFile{" +
                "filepath='" + filepath + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvPaymentsFile that = (CsvPaymentsFile) o;
        return getFilepath().equals(that.getFilepath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilepath());
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
