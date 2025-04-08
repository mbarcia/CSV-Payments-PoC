package com.example.poc.repository;

import com.example.poc.domain.CsvFolder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CsvFolderRepository extends CrudRepository<CsvFolder, UUID> {
}