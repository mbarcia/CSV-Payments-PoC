package com.example.poc.common.domain;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class CsvPaymentsOutputFile extends BaseCsvPaymentsFile implements AutoCloseable {

  @Transient private final List<PaymentOutput> paymentOutputs = new ArrayList<>();

  @Transient StatefulBeanToCsv<PaymentOutput> sbc;

  @Transient private Writer writer;

  public CsvPaymentsOutputFile(@NonNull String csvPaymentsInputFilepath) throws IOException {
    super();
    assignFileAndFolder(new File(MessageFormat.format("{0}.out", csvPaymentsInputFilepath)));
    // Create the CSV writer
    writer = new BufferedWriter(new FileWriter(this.getFilepath()));
    sbc =
        new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
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

  @Override
  public void close() throws Exception {
    writer.close();
  }
}
