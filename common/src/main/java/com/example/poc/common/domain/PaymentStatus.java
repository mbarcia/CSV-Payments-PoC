package com.example.poc.common.domain;

import static java.text.MessageFormat.format;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class PaymentStatus extends BaseEntity implements Serializable {
  private String customerReference;

  @NonNull
  @Column(nullable = false)
  private String reference;

  @NonNull private String status;
  @NonNull private String message;
  @NonNull private BigDecimal fee;

  @Transient private AckPaymentSent ackPaymentSent;
  @NonNull private UUID ackPaymentSentId;

  @Transient private PaymentRecord paymentRecord;
  @NonNull private UUID paymentRecordId;

  @Override
  public String toString() {
    return format(
        "PaymentStatus'{'customerReference=''{0}'', reference=''{1}'', message=''{2}'', status={3}, fee={4}, recordId={5}'}'",
        customerReference, reference, message, status, fee, paymentRecordId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PaymentStatus that = (PaymentStatus) o;
    return this.getId() != null && this.getId().equals(that.getId());
  }
}
