package com.example.poc.dto;

import com.example.poc.dto.PaymentRecordDto;

public class AckPaymentSentDto {
    public String id;
    public String conversationId;
    public String paymentRecordId;
    public PaymentRecordDto paymentRecord;
}
