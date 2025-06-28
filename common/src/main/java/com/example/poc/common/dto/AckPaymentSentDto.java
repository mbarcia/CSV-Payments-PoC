package com.example.poc.common.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AckPaymentSentDto {
    public UUID id;
    public UUID conversationId;
    public UUID paymentRecordId;
    public PaymentRecordDto paymentRecord;
}
