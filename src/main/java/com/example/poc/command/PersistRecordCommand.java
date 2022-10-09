package com.example.poc.command;

import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PersistRecordCommand extends BaseCommand<PaymentRecord, PaymentRecord> {
    @Autowired
    PaymentRecordRepository paymentRecordRepository;

    @Override
    public PaymentRecord execute(PaymentRecord processableObj) {
        super.execute(processableObj);

        paymentRecordRepository.save(processableObj);
        return processableObj;
    }
}
