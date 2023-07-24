package com.example.poc.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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
}
