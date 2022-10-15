package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.AckPaymentSentRepository;
import com.example.poc.repository.CsvFolderRepository;
import com.example.poc.repository.CsvPaymentsFileRepository;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CsvPaymentsServiceImpl implements CsvPaymentsService {
    @Autowired
    CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Autowired
    CsvFolderRepository csvFolderRepository;

    @Autowired
    AckPaymentSentRepository ackPaymentSentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    public CsvPaymentsServiceImpl(CsvPaymentsFileRepository csvPaymentsFileRepository, CsvFolderRepository csvFolderRepository, AckPaymentSentRepository ackPaymentSentRepository, PaymentRecordRepository paymentRecordRepository) {
        this.csvPaymentsFileRepository = csvPaymentsFileRepository;
        this.csvFolderRepository = csvFolderRepository;
        this.ackPaymentSentRepository = ackPaymentSentRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Override
    public Optional<CsvPaymentsFile> findFileById(Long id) {
        return csvPaymentsFileRepository.findById(id);
    }

    @Override
    public Optional<CsvFolder> findFolderById(Long id) {
        return csvFolderRepository.findById(id);
    }

    @Override
    public Optional<AckPaymentSent> findAckPaymentSentByRecord(PaymentRecord record) {
        return ackPaymentSentRepository.findByRecord(record);
    }

    @Override
    public PaymentRecord persistRecord(PaymentRecord record) {
        return paymentRecordRepository.save(record);
    }

    @Override
    public CsvFolder persistFolder(CsvFolder csvFolder) {
        return csvFolderRepository.save(csvFolder);
    }

    @Override
    public CsvPaymentsFile persistFile(CsvPaymentsFile csvPaymentsFile) {
        return csvPaymentsFileRepository.save(csvPaymentsFile);
    }

    @Override
    public AckPaymentSent save(AckPaymentSent ackPaymentSent) {
        return ackPaymentSentRepository.save(ackPaymentSent);
    }
}
