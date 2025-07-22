package com.example.poc.common.dto;

import com.example.poc.common.domain.PaymentRecord;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AckPaymentSentDto {
    public UUID id;
    public UUID conversationId;
    public UUID paymentRecordId;
    public PaymentRecord paymentRecord;
    public String message;
    public Long status;
}
