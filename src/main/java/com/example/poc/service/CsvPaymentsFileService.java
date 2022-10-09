package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CsvPaymentsFileService {
    @Autowired
    CsvPaymentsFileRepository csvPaymentsFileRepository;
    public Optional<CsvPaymentsFile> findById(Long id) {
        return csvPaymentsFileRepository.findById(id);
    }
}
