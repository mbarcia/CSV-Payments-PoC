package com.example.poc.service;

import com.example.poc.domain.*;

import java.util.Optional;

public interface CsvPaymentsService {
    Optional<CsvPaymentsFile> findFileById(Long id);

    Optional<CsvFolder> findFolderById(Long id);

    Optional<AckPaymentSent> findAckPaymentSentByRecord(PaymentRecord record);

    PaymentRecord persistRecord(PaymentRecord record);

    CsvFolder persistFolder(CsvFolder csvFolder);

    CsvPaymentsFile persistFile(CsvPaymentsFile csvPaymentsFile);

    AckPaymentSent persistAckPaymentSent(AckPaymentSent ackPaymentSent);

    PaymentStatus persistPaymentStatus(PaymentStatus paymentStatus);
}
