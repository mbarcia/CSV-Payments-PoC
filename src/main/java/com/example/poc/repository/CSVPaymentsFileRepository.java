package com.example.poc.repository;

import com.example.poc.biz.CSVPaymentsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CSVPaymentsFileRepository extends JpaRepository<CSVPaymentsFile, Long> {
    @Override
    Optional<CSVPaymentsFile> findById(Long aLong);
}