package com.example.poc.domain;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import static java.text.MessageFormat.format;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseCsvPaymentsFile implements Serializable {
    @Id
    @CsvIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    @NonNull
    protected File csvFile;
    protected String filepath;

    @ManyToOne(fetch = FetchType.LAZY)
    private CsvFolder csvFolder;

    @Override
    public String toString() {
        return format("CsvPaymentsFile'{'filepath=''{0}'''}'", filepath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvPaymentsInputFile that = (CsvPaymentsInputFile) o;
        return getFilepath().equals(that.getFilepath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilepath());
    }
}
