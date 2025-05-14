package com.example.poc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Mapper(componentModel = "cdi")
public interface CommonConverters {
    @Named("stringToUUID")
    static UUID toUUID(String id) {
        return id == null || id.isBlank() ? null : UUID.fromString(id);
    }

    @Named("uuidToString")
    static String toString(UUID id) {
        return id == null ? "" : id.toString();
    }

    @Named("bigDecimalToString")
    static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }

    @Named("stringToBigDecimal")
    static BigDecimal stringToBigDecimal(String str) {
        return (str != null && !str.isEmpty()) ? new BigDecimal(str) : BigDecimal.ZERO;
    }

    @Named("currencyToString")
    static String currencyToString(Currency currency) {
        return currency != null ? currency.getCurrencyCode() : "";
    }

    @Named("stringToCurrency")
    static Currency stringToCurrency(String str) {
        return (str != null && !str.isEmpty()) ? Currency.getInstance(str) : null;
    }
}
