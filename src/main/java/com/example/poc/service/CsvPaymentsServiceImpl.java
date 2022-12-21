package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.repository.*;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
