package com.example.poc.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Objects;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent extends BasePersistable {

    @NonNull
    private String conversationID;

    private Long status;
    private String message;

    @OneToOne(fetch = FetchType.EAGER)
    private PaymentRecord record;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "ackPaymentSent")
    @Getter
    @Setter
    private PaymentStatus paymentStatus;

    @Override
    public String toString() {
        return "AckPaymentSent{" +
                "id='" + id + '\'' +
                ", conversationID='" + conversationID + '\'' +
                ", status=" + status +
                ", message=" + message +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AckPaymentSent ackPaymentSent = (AckPaymentSent) o;
        return (conversationID.equals(ackPaymentSent.getConversationID()) &&
                message.equals(ackPaymentSent.getMessage()) &&
                status.equals(ackPaymentSent.getStatus()) &&
                record.equals(ackPaymentSent.getRecord()) &&
                Objects.equals(paymentStatus, ackPaymentSent.getPaymentStatus())
        );
    }
}
