package com.example.poc.domain;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.*;
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
    @Transient
    StatefulBeanToCsv<PaymentOutput> sbc;
    @Transient
    private FileWriter writer;
    @Transient
    private Reader reader;
    @Transient
    CsvToBean<PaymentRecord> csvReader;

    public CsvPaymentsFile(@NonNull File csvFile) throws IOException {
        this.csvFile = csvFile;
        filepath = csvFile.getPath();
        // Create the CSV writer
        writer = new FileWriter(STR."\{filepath}.out");
        sbc = new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
                .withQuotechar('\'')
                .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                .build();
        reader = new BufferedReader(new FileReader(filepath));
        csvReader = new CsvToBeanBuilder<PaymentRecord>(reader)
                .withType(PaymentRecord.class)
                .withSeparator(',')
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();
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
