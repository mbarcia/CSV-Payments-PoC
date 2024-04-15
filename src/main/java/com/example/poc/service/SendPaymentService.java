package com.example.poc.service;

import com.example.poc.command.SendPaymentCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class SendPaymentService extends BaseService<PaymentRecord, AckPaymentSent> {
    public SendPaymentService(PaymentRecordRepository repository, SendPaymentCommand command) {
        super(repository, command);
    }
}
