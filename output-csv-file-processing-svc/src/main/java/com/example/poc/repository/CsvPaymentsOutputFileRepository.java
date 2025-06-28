package com.example.poc.repository;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CsvPaymentsOutputFileRepository implements PanacheRepository<CsvPaymentsOutputFile> {
}
