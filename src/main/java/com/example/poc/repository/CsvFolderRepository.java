package com.example.poc.repository;

import com.example.poc.domain.CsvFolder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvFolderRepository extends CrudRepository<CsvFolder, Long> {
}