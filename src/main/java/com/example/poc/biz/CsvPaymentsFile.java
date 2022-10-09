package com.example.poc.biz;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CsvPaymentsFile {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "csvPaymentsFile"
    )
    private List<PaymentRecord> records = new ArrayList<>();

    @Getter @Setter
    private File csvFile;

    @Getter @Setter
    @ManyToOne
    private CsvFolder csvFolder;

    @Getter @Setter
    private String filepath;

    @Getter @Setter
    private Timestamp startTimestamp;

    protected CsvPaymentsFile() {
    }

    public CsvPaymentsFile(File csvFile) {
        this.csvFile = csvFile;
        this.filepath = csvFile.getPath();
        setStartTimestamp(Timestamp.from(Instant.now()));
    }

    @Override
    public String toString() {
        return "CsvPaymentsFile{" +
                "filepath='" + filepath + '\'' +
                '}';
    }
}
