package com.example.poc.service;

import com.example.poc.command.SendPaymentRecordCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.service.BasePersistedReactiveService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class SendPaymentRecordReactiveService extends BasePersistedReactiveService<PaymentRecord, AckPaymentSent> {
    @Inject
    SendPaymentRecordCommand command;
}
