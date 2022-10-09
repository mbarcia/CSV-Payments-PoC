package com.example.poc.repository;

import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CsvPaymentsFileRepository extends JpaRepository<CsvPaymentsFile, Long> {
    @Override
    Optional<CsvPaymentsFile> findById(Long aLong);
}
