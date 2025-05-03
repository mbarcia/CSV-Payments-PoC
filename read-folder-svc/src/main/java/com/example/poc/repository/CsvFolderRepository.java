package com.example.poc.repository;

import com.example.poc.domain.CsvFolder;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CsvFolderRepository implements PanacheRepository<CsvFolder> {
}
