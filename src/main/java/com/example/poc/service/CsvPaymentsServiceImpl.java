package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.repository.*;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
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
    public AckPaymentSent persistAckPaymentSent(AckPaymentSent ackPaymentSent) {
        return ackPaymentSentRepository.save(ackPaymentSent);
    }

    @Override
    public PaymentStatus persistPaymentStatus(PaymentStatus paymentStatus) {
        return paymentStatusRepository.save(paymentStatus);
    }
}
