package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Getter @Setter
public class PaymentStatus implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    private String customerReference;

    @Column(nullable = false)
    private String reference;
    private String status;
    private String message;
    private BigDecimal fee;

    @OneToOne(fetch = FetchType.EAGER)
    private AckPaymentSent ackPaymentSent;

    @Override
    public String toString() {
        return "PaymentStatus{" +
                "customerReference='" + customerReference + '\'' +
                ", reference='" + reference + '\'' +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", fee=" + fee +
                '}';
    }
}
