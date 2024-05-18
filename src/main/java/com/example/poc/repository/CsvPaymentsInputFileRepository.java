package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsInputFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvPaymentsInputFileRepository extends CrudRepository<CsvPaymentsInputFile, Long> {
}
