package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsOutputFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CsvPaymentsOutputFileRepository extends CrudRepository<CsvPaymentsOutputFile, UUID> {
}
