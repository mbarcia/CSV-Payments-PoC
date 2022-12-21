package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

import java.math.BigDecimal;
import java.util.Currency;

public record PaymentOutput(
        @CsvBindByName(column = "CSV Id") String csvId,
        @CsvBindByName(column = "Recipient") String recipient,
        @CsvBindByName(column = "Amount") @CsvNumber("#,###.00") BigDecimal amount,
        @CsvBindByName(column = "Currency") Currency currency,
        @CsvBindByName(column = "Reference") String conversationID,
        @CsvBindByName(column = "Status") Long status,
        @CsvBindByName(column = "Message") String message,
        @CsvBindByName(column = "Fee") @CsvNumber("#,###.00") BigDecimal fee) {
}
