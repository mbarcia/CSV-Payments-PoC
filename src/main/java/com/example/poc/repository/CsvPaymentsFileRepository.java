package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvPaymentsFileRepository extends CrudRepository<CsvPaymentsFile, Long> {
}
