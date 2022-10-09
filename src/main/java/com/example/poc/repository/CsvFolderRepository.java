package com.example.poc.repository;

import com.example.poc.biz.CsvFolder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvFolderRepository extends JpaRepository<CsvFolder, Long> {
}