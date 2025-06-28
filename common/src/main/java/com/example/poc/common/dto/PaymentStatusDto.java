package com.example.poc.common.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class PaymentStatusDto {
    public UUID id;
    public String reference;
    public String status;
    public String message;
    public BigDecimal fee;
    public UUID ackPaymentSentId;
    public AckPaymentSentDto ackPaymentSent;
}
