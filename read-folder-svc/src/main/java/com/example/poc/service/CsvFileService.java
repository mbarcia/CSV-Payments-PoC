package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;

import java.io.File;
import java.io.IOException;

public interface CsvFileService {
    File[] listCsvFiles(String directoryPath);

    CsvPaymentsInputFile createInputCsvFile(File file);

    CsvPaymentsOutputFile createOutputCsvFile(CsvPaymentsInputFile inputFile) throws IOException;
}
