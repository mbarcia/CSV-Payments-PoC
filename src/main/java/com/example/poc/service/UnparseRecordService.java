package com.example.poc.service;

import com.example.poc.command.UnparseRecordCommand;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class UnparseRecordService extends BaseService<PaymentStatus, PaymentOutput> {
    public UnparseRecordService(CrudRepository<PaymentStatus, Long> repository, UnparseRecordCommand command) {
        super(repository, command);
    }
}
