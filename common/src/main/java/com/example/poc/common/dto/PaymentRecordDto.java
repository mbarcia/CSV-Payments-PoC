package com.example.poc.common.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Value
@Builder
public class PaymentRecordDto {
    public UUID id;
    public String csvId;
    public String recipient;
    public BigDecimal amount;
    public Currency currency;
    public String csvPaymentsInputFilePath;
}
