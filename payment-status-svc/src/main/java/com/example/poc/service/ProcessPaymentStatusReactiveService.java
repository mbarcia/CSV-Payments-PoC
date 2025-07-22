package com.example.poc.service;

import com.example.poc.command.ProcessPaymentStatusCommand;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.BasePersistedReactiveService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class ProcessPaymentStatusReactiveService extends BasePersistedReactiveService<PaymentStatus, PaymentOutput> {
    @Inject
    ProcessPaymentStatusCommand command;
}
