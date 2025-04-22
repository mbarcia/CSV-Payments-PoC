package com.example.poc.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

import static java.text.MessageFormat.format;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent extends BaseEntity implements Serializable {
    @NonNull
    private String conversationID;

    private Long status;
    private String message;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "ackPaymentSent")
    private PaymentStatus paymentStatus;

    @Override
    public String toString() {
        return format("AckPaymentSent'{'id=''{0}'', conversationID=''{1}'', status={2}, message={3}'}'", getId(), conversationID, status, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AckPaymentSent that = (AckPaymentSent) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
