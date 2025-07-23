package com.example.poc.common.dto;

import com.example.poc.common.domain.PaymentStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentOutputDto implements Serializable {

  UUID id;

  String csvId;
  String recipient;
  BigDecimal amount;
  Currency currency;
  String conversationId;
  Long status;
  String message;
  BigDecimal fee;
  PaymentStatus paymentStatus;
}
