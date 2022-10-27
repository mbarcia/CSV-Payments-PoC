package com.example.poc.repository;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CsvPaymentsFileRepository extends JpaRepository<CsvPaymentsFile, Long> {
    @Query("select file from CsvPaymentsFile file where file.csvFolder = ?1")
    List<CsvPaymentsFile> findAllByFolder(CsvFolder csvFolder);
}
