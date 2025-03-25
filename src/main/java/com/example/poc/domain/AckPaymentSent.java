package com.example.poc.domain;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import static java.text.MessageFormat.format;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent {
    @Id
    @CsvIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String conversationID;

    private Long status;
    private String message;

    @OneToOne(fetch = FetchType.EAGER)
    private PaymentRecord record;

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
        AckPaymentSent ackPaymentSent = (AckPaymentSent) o;
        return id != null && id.equals(ackPaymentSent.id);
    }
}
