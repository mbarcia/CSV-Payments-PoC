package com.example.poc.biz;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
public class CSVPaymentsFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    private File file;

    private String filepath;

    public Timestamp getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Timestamp startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    private Timestamp startTimestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    protected CSVPaymentsFile() {
    }

    public CSVPaymentsFile(File file) {
        this.file = file;
        this.filepath = file.getPath();
        setStartTimestamp(Timestamp.from(Instant.now()));
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String toString() {
        return "CSVPaymentsFile{" +
                "filepath='" + filepath + '\'' +
                '}';
    }
}
