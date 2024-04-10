package com.example.poc.service;

import com.example.poc.command.ProcessRecordCommand;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class ProcessRecordService extends BaseService<PaymentRecord, PaymentOutput> {
    public ProcessRecordService(CrudRepository<PaymentRecord, Long> repository, ProcessRecordCommand command) {
        super(repository, command);
    }
}
