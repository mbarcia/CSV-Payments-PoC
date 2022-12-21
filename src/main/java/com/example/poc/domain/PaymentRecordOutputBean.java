package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Setter
public class PaymentRecordOutputBean {
    private String csvId;
    private String recipient;
    private BigDecimal amount;
    private Currency currency;
    private String conversationID;
    private Long status;
    private String message;
    private BigDecimal fee;
}