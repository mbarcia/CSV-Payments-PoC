package com.example.poc.common.dto;

import com.example.poc.common.domain.AckPaymentSent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentStatusDto {
  public UUID id;
  public String reference;
  public String status;
  public String message;
  public BigDecimal fee;
  public UUID ackPaymentSentId;
  public AckPaymentSent ackPaymentSent;
}
