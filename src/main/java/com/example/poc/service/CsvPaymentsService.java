package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;

import java.util.Optional;

public interface CsvPaymentsService {
    Optional<CsvPaymentsFile> findFileById(Long id);

    Optional<CsvFolder> findFolderById(Long id);

    Optional<AckPaymentSent> findAckPaymentSentByRecord(PaymentRecord record);

    PaymentRecord persistRecord(PaymentRecord record);

    CsvFolder persistFolder(CsvFolder csvFolder);

    CsvPaymentsFile persistFile(CsvPaymentsFile csvPaymentsFile);

    AckPaymentSent save(AckPaymentSent ackPaymentSent);
}
