package com.example.poc.command;

import com.example.poc.client.PaymentProviderClient;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import com.example.poc.service.CsvPaymentsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PollPaymentCommand extends BaseCommand<CsvPaymentsFile, CsvPaymentsFile> {
    @Autowired
    PaymentProviderClient client;

    @Autowired
    CsvPaymentsService service;

    @Autowired
    PaymentStatusRepository paymentStatusRepository;

    @Async
    public CsvPaymentsFile execute(CsvPaymentsFile aFile) {
        super.execute(aFile);

        List<PaymentRecord> processedFileData = aFile.getRecords();

        try {
            Thread.sleep(6000);

            for (PaymentRecord record : processedFileData) {
                PaymentStatus paymentStatus;
//                TODO
                paymentStatus = client.getPaymentStatus(record.getAckPaymentSent());
//                paymentStatus = new PaymentStatus();
//                paymentStatus.setStatus("nada");
//                paymentStatus.setFee(java.math.BigDecimal.valueOf(1.01));
//                paymentStatus.setMessage("This is a test");
                Optional<AckPaymentSent> ackPaymentSent = service.findAckPaymentSentByRecord(record);
                ackPaymentSent.ifPresent(ack -> {
                    paymentStatus.setReference(ack.getConversationID());
                    paymentStatus.setAckPaymentSent(ack);
                });

                paymentStatusRepository.save(paymentStatus);
            }

        } catch (InterruptedException | JsonProcessingException ie) {
//        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        return aFile;
    }
}
