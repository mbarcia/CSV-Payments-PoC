package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsInputFile;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CsvPaymentsInputFileRepository implements PanacheRepository<CsvPaymentsInputFile> {
}
