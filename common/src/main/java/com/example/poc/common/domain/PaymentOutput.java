package com.example.poc.common.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvNumber;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PaymentOutput extends BaseEntity implements Serializable {

  @Setter(AccessLevel.NONE) // Avoids override by MapStruct
  private UUID id;

  @CsvIgnore @Transient private PaymentStatus paymentStatus;

  // en-UK locale to match the format of the (mock) payment service
  @CsvBindByName(column = "CSV Id")
  String csvId;

  @CsvBindByName(column = "Recipient")
  String recipient;

  @CsvBindByName(column = "Amount", locale = "en-UK")
  @CsvNumber("#,###.00")
  BigDecimal amount;

  @CsvBindByName(column = "Currency")
  Currency currency;

  @CsvBindByName(column = "Reference")
  UUID conversationId;

  @CsvBindByName(column = "Status")
  Long status;

  @CsvBindByName(column = "Message")
  String message;

  @CsvBindByName(column = "Fee", locale = "en-UK")
  @CsvNumber("#,###.00")
  BigDecimal fee;

  public PaymentOutput(
      PaymentStatus paymentStatus,
      String csvId,
      String recipient,
      BigDecimal amount,
      Currency currency,
      UUID conversationId,
      Long status,
      String message,
      BigDecimal fee) {
    super();
    this.paymentStatus = paymentStatus;
    this.csvId = csvId;
    this.recipient = recipient;
    this.amount = amount;
    this.currency = currency;
    this.conversationId = conversationId;
    this.status = status;
    this.message = message;
    this.fee = fee;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PaymentOutput that = (PaymentOutput) o;
    return this.getId() != null && this.getId().equals(that.getId());
  }
}
