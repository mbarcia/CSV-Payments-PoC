package com.example.poc.client;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Currency;

@Setter
@Getter
@Accessors(chain = true)
public class SendPaymentRequest {
    private String msisdn;
    private BigDecimal amount;
    private Currency currency;
    private String reference;
    private String url;
}
