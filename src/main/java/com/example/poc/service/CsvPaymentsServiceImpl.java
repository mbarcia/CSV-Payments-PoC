package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CsvPaymentsServiceImpl implements CsvPaymentsService {
    private final CsvPaymentsFileRepository csvPaymentsFileRepository;
    private final CsvFolderRepository csvFolderRepository;
    private final AckPaymentSentRepository ackPaymentSentRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentStatusRepository paymentStatusRepository;

    @Autowired
    public CsvPaymentsServiceImpl(CsvPaymentsFileRepository csvPaymentsFileRepository, CsvFolderRepository csvFolderRepository, AckPaymentSentRepository ackPaymentSentRepository, PaymentRecordRepository paymentRecordRepository, PaymentStatusRepository paymentStatusRepository) {
        this.csvPaymentsFileRepository = csvPaymentsFileRepository;
        this.csvFolderRepository = csvFolderRepository;
        this.ackPaymentSentRepository = ackPaymentSentRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @Override
    public PaymentRecord persist(PaymentRecord record) {
        return paymentRecordRepository.save(record);
    }

    @Override
    public CsvFolder persist(CsvFolder csvFolder) {
        return csvFolderRepository.save(csvFolder);
    }

    @Override
    public CsvPaymentsFile persist(CsvPaymentsFile csvPaymentsFile) {
        return csvPaymentsFileRepository.save(csvPaymentsFile);
    }

    @Override
    public AckPaymentSent persist(AckPaymentSent ackPaymentSent) {
        return ackPaymentSentRepository.save(ackPaymentSent);
    }

    @Override
    public PaymentStatus persist(PaymentStatus paymentStatus) {
        return paymentStatusRepository.save(paymentStatus);
    }
}
