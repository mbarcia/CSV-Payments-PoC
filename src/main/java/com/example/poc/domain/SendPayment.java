package com.example.poc.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@Table(name = "send_payment")
public class SendPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "conversation_id", nullable = false)
    private String conversationID;
    private Long status;
    private String message;

    @OneToOne(cascade=CascadeType.ALL)
    private PaymentRecord record;

    @Override
    public String toString() {
        return "SendPayment{" +
                "id='" + id + '\'' +
                ", conversationID='" + conversationID + '\'' +
                ", status=" + status +
                ", message=" + message +
                '}';
    }
}
