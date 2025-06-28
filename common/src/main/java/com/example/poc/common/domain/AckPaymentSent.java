package com.example.poc.common.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.UUID;

import static java.text.MessageFormat.format;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent extends BaseEntity implements Serializable {
    @NonNull
    private String conversationId;

    private Long status;
    private String message;

    @Transient
    private PaymentRecord paymentRecord;
    private UUID paymentRecordId;

    @Override
    public String toString() {
        return format("AckPaymentSent'{'id=''{0}'', conversationId=''{1}'', status={2}, message={3}, recordId={4}'}'", getId(), conversationId, status, message, paymentRecordId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AckPaymentSent that = (AckPaymentSent) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
