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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class CsvPaymentsOutputFile extends BaseCsvPaymentsFile {

    @Transient
    private final List<PaymentOutput> paymentOutputs = new ArrayList<>();

    @Transient
    StatefulBeanToCsv<PaymentOutput> sbc;
    @Transient
    private Writer writer;

    public CsvPaymentsOutputFile(@NonNull CsvPaymentsInputFile csvPaymentsInputFile) throws IOException {
        this.filepath = MessageFormat.format("{0}.out", csvPaymentsInputFile.getFilepath());
        this.csvFile = new File(this.getFilepath());
        // Create the CSV writer
        writer = new BufferedWriter(new FileWriter(this.getFilepath()));
        sbc = new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
                .withQuotechar('\'')
                .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvPaymentsOutputFile that = (CsvPaymentsOutputFile) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
