package com.example.poc.service;

import com.example.poc.command.PollAckPaymentSentCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.BaseReactiveService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

@ApplicationScoped
@Getter
public class PollAckPaymentSentReactiveService extends BaseReactiveService<AckPaymentSent, PaymentStatus> {
    @Inject
    PollAckPaymentSentCommand command;
}
