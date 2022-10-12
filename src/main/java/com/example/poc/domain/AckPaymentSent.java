package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
public class AckPaymentSent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String conversationID;

    private Long status;
    private String message;

    @OneToOne(fetch = FetchType.EAGER)
    private PaymentRecord record;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "ackPaymentSent")
    @Getter @Setter
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
