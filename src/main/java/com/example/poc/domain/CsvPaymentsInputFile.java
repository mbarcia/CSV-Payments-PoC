package com.example.poc.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class CsvPaymentsInputFile extends BaseCsvPaymentsFile {
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "csvPaymentsInputFile",
            orphanRemoval = true
    )
    private final List<PaymentRecord> records = new ArrayList<>();

    public CsvPaymentsInputFile(@NonNull File csvFile) {
        this.csvFile = csvFile;
        filepath = csvFile.getPath();
    }
}
