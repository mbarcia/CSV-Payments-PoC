package com.example.poc.service;

import com.example.poc.command.ProcessAckPaymentSentCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.BasePersistedReactiveService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class ProcessAckPaymentSentReactiveService extends BasePersistedReactiveService<AckPaymentSent, PaymentStatus> {
    @Inject
    ProcessAckPaymentSentCommand command;
}
