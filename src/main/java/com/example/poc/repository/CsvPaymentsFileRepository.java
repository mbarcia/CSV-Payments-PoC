package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvPaymentsFileRepository extends JpaRepository<CsvPaymentsFile, Long> {
}
