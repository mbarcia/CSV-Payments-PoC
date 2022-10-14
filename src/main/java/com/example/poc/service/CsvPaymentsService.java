package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.AckPaymentSentRepository;
import com.example.poc.repository.CsvFolderRepository;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CsvPaymentsService {
    @Autowired
    CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Autowired
    CsvFolderRepository csvFolderRepository;

    @Autowired
    AckPaymentSentRepository ackPaymentSentRepository;

    public Optional<CsvPaymentsFile> findFileById(Long id) {
        return csvPaymentsFileRepository.findById(id);
    }

    public Optional<CsvFolder> findFolderById(Long id) {
        return csvFolderRepository.findById(id);
    }

    public Optional<AckPaymentSent> findAckPaymentSentByRecord(PaymentRecord record) {
        return ackPaymentSentRepository.findByRecord(record);
    }
}
