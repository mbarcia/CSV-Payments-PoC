package com.example.poc.service;

import com.example.poc.command.SendPaymentRecordCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.service.LocalAbstractServiceWithAudit;
import com.example.poc.repository.PaymentRecordRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SendPaymentRecordService extends LocalAbstractServiceWithAudit<PaymentRecord, AckPaymentSent> {
    public SendPaymentRecordService(PaymentRecordRepository repository, SendPaymentRecordCommand command) {
        super(repository, command);
    }
}
