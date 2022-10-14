package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@Accessors(chain = true)
public class CsvPaymentsFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "csvPaymentsFile",
            orphanRemoval = true
    )
    private final List<PaymentRecord> records = new ArrayList<>();

    @Transient
    private File csvFile;

    @ManyToOne
    private CsvFolder csvFolder;

    private String filepath;

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
