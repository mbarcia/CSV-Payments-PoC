package com.example.poc.service;

import com.example.poc.command.SendPaymentRecordCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class SendPaymentRecordService extends BaseServiceWithAudit<PaymentRecord, AckPaymentSent> {
    public SendPaymentRecordService(PaymentRecordRepository repository, SendPaymentRecordCommand command) {
        super(repository, command);
    }
}
