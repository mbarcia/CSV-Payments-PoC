package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.repository.*;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@NoArgsConstructor
public class CsvPaymentsServiceImpl implements CsvPaymentsService {
    @Autowired
    private CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Autowired
    private CsvFolderRepository csvFolderRepository;

    @Autowired
    private AckPaymentSentRepository ackPaymentSentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private PaymentStatusRepository paymentStatusRepository;

    @Override
    public Optional<CsvPaymentsFile> findFileById(Long id) {
        return csvPaymentsFileRepository.findById(id);
    }

    @Override
    public Optional<CsvFolder> findFolderById(Long id) {
        return csvFolderRepository.findById(id);
    }

    @Transactional
    @Override
    public PaymentRecord persistRecord(PaymentRecord record) {
        return paymentRecordRepository.save(record);
    }

    @Transactional
    @Override
    public CsvFolder persistFolder(CsvFolder csvFolder) {
        return csvFolderRepository.save(csvFolder);
    }

    @Transactional
    @Override
    public CsvPaymentsFile persistFile(CsvPaymentsFile csvPaymentsFile) {
        return csvPaymentsFileRepository.save(csvPaymentsFile);
    }

    @Override
    public AckPaymentSent persistAckPaymentSent(AckPaymentSent ackPaymentSent) {
        return ackPaymentSentRepository.save(ackPaymentSent);
    }

    @Transactional
    @Override
    public PaymentStatus persistPaymentStatus(PaymentStatus paymentStatus) {
        return paymentStatusRepository.save(paymentStatus);
    }

    @Override
    public List<CsvPaymentsFile> findFilesByFolder(CsvFolder csvFolder) {
        return csvPaymentsFileRepository.findAllByFolder(csvFolder);
    }

    @Override
    public List<PaymentRecord> findRecordsByFile(CsvPaymentsFile csvPaymentsFile) {
        return paymentRecordRepository.findAllByFile(csvPaymentsFile);
    }
}
