package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CsvFolder {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter @Setter
    private String folderPath;

    @Getter
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "csvFolder",
            orphanRemoval = true
    )
    private final List<CsvPaymentsFile> files = new ArrayList<>();

    public CsvFolder() {
    }

    public CsvFolder(String folderPath) {
        this.folderPath = folderPath;
    }

    public String toString() {
        return getFolderPath();
    }
}
