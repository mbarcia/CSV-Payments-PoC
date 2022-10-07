package com.example.poc.command;

import com.example.poc.Command;
import com.example.poc.biz.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersistRecordCommand implements Command<PaymentRecord, PaymentRecord> {
    @Autowired
    PaymentRecordRepository paymentRecordRepository;

    @Override
    public PaymentRecord execute(PaymentRecord processableObj) {
        paymentRecordRepository.save(processableObj);
        return processableObj;
    }
}
