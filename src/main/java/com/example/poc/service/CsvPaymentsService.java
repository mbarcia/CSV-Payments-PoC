package com.example.poc.service;

import com.example.poc.domain.*;

import java.util.List;
import java.util.Optional;

public interface CsvPaymentsService {
    Optional<CsvPaymentsFile> findFileById(Long id);

    Optional<CsvFolder> findFolderById(Long id);

    PaymentRecord persistRecord(PaymentRecord record);

    CsvFolder persistFolder(CsvFolder csvFolder);

    CsvPaymentsFile persistFile(CsvPaymentsFile csvPaymentsFile);

    AckPaymentSent persistAckPaymentSent(AckPaymentSent ackPaymentSent);

    PaymentStatus persistPaymentStatus(PaymentStatus paymentStatus);

    List<CsvPaymentsFile> findFilesByFolder(CsvFolder csvFolder);

    List<PaymentRecord> findRecordsByFile(CsvPaymentsFile csvPaymentsFile);
}
