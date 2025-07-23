package com.example.poc.common.mapper;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "cdi")
public class CommonConverters {
  @Named("stringToUUID")
  public UUID toUUID(String id) {
    return id == null || id.isBlank() ? null : UUID.fromString(id);
  }

  @Named("uuidToString")
  public String toString(UUID id) {
    return id == null ? "" : id.toString();
  }

  @Named("bigDecimalToString")
  public String bigDecimalToString(BigDecimal value) {
    return value != null ? value.toPlainString() : "0";
  }

  @Named("stringToBigDecimal")
  public BigDecimal stringToBigDecimal(String str) {
    return (str != null && !str.isEmpty()) ? new BigDecimal(str) : BigDecimal.ZERO;
  }

  @Named("longToString")
  public String longToString(Long value) {
    return value != null ? value.toString() : "0";
  }

  @Named("stringToLong")
  public Long stringToLong(String str) {
    return (str != null && !str.isEmpty()) ? Long.parseLong(str) : 0L;
  }

  @Named("currencyToString")
  public String currencyToString(Currency currency) {
    return currency != null ? currency.getCurrencyCode() : "";
  }

  @Named("stringToCurrency")
  public Currency stringToCurrency(String str) {
    return (str != null && !str.isEmpty()) ? Currency.getInstance(str) : null;
  }
}
