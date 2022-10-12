package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PollPaymentCommand extends BaseCommand<CsvPaymentsFile, CsvPaymentsFile> {
    @Autowired
    PaymentProviderClient client;

    @Autowired
    PaymentStatusRepository paymentStatusRepository;

    @Transactional
    @Async
    public CsvPaymentsFile execute(CsvPaymentsFile aFile) {
        List<PaymentRecord> processedFileData = aFile.getRecords();

        try {
            Thread.sleep(6000);

            for (PaymentRecord record: processedFileData) {
                PaymentStatus paymentStatus = client.getPaymentStatus(record.getSendPayment().getConversationID());
                paymentStatusRepository.save(paymentStatus);
            }

        } catch (InterruptedException e) {
            //
        }

        return aFile;
    }
}
