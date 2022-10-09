package com.example.poc.service;

import com.example.poc.domain.CsvFolder;
import com.example.poc.repository.CsvFolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CsvFolderService {
    @Autowired
    CsvFolderRepository csvFolderRepository;
    public Optional<CsvFolder> findById(Long id) {
        return csvFolderRepository.findById(id);
    }
}
