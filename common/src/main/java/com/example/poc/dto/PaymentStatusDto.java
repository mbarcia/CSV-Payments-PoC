package com.example.poc.dto;

import com.example.poc.dto.AckPaymentSentDto;

public class PaymentStatusDto {
    public String id;
    public String reference;
    public String status;
    public String message;
    public String fee;
    public String ackPaymentSentId;
    public AckPaymentSentDto ackPaymentSent;
}
