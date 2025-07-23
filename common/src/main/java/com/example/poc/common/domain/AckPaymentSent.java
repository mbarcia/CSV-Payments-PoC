package com.example.poc.common.domain;

import static java.text.MessageFormat.format;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class AckPaymentSent extends BaseEntity implements Serializable {
  @NonNull private UUID conversationId;

  private Long status;
  private String message;

  @Transient private PaymentRecord paymentRecord;
  private UUID paymentRecordId;

  public AckPaymentSent(@NonNull UUID conversationId) {
    super();
    this.conversationId = conversationId;
  }

  @Override
  public String toString() {
    return format(
        "AckPaymentSent'{'id=''{0}'', conversationId=''{1}'', status={2}, message={3}, recordId={4}'}'",
        getId(), conversationId, status, message, paymentRecordId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AckPaymentSent that = (AckPaymentSent) o;
    return this.getId() != null && this.getId().equals(that.getId());
  }
}
