package com.example.poc.domain;

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

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class CsvPaymentsOutputFile extends BaseCsvPaymentsFile implements AutoCloseable {
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "csvPaymentsOutputFile",
            orphanRemoval = true
    )
    private final List<PaymentOutput> paymentOutputs = new ArrayList<>();

    @Transient
    StatefulBeanToCsv<PaymentOutput> sbc;
    @Transient
    private Writer writer;

    public CsvPaymentsOutputFile(@NonNull CsvPaymentsInputFile csvPaymentsInputFile) throws IOException {
        this.filepath = STR."\{csvPaymentsInputFile.getFilepath()}.out";
        this.csvFile = new File(this.getFilepath());
        // Create the CSV writer
        writer = new BufferedWriter(new FileWriter(this.getFilepath()));
        sbc = new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
                .withQuotechar('\'')
                .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                .build();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
