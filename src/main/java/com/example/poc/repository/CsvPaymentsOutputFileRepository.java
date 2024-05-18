package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsOutputFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvPaymentsOutputFileRepository extends CrudRepository<CsvPaymentsOutputFile, Long> {
}
