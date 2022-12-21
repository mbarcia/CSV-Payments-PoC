package com.example.poc.service;

import com.example.poc.domain.*;

public interface CsvPaymentsService {
    PaymentRecord persist(PaymentRecord record);

    CsvFolder persist(CsvFolder csvFolder);

    CsvPaymentsFile persist(CsvPaymentsFile csvPaymentsFile);

    AckPaymentSent persist(AckPaymentSent ackPaymentSent);

    PaymentStatus persist(PaymentStatus paymentStatus);
}
