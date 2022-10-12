package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

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
    private final Set<CsvPaymentsFile> files = new LinkedHashSet<>();

    public CsvFolder() {
    }

    public CsvFolder(String folderPath) {
        this.folderPath = folderPath;
    }

    public String toString() {
        return getFolderPath();
    }
}
