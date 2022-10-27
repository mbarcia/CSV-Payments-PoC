package com.example.poc.repository;

import com.example.poc.domain.CsvFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvFolderRepository extends JpaRepository<CsvFolder, Long> {
}