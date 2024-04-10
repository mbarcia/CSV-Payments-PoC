package com.example.poc.domain;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class PaymentStatus implements Serializable {
    @Id
    @CsvIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerReference;

    @NonNull
    @Column(nullable = false)
    private String reference;
    private String status;
    private String message;
    private BigDecimal fee;

    @OneToOne(fetch = FetchType.LAZY)
    private AckPaymentSent ackPaymentSent;

    @Override
    public String toString() {
        return STR."PaymentStatus{customerReference='\{customerReference}\{'\''}, reference='\{reference}\{'\''}, message='\{message}\{'\''}, status=\{status}, fee=\{fee}\{'}'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentStatus that = (PaymentStatus) o;
        return (reference.equals(that.getReference()) &&
            Objects.equals(customerReference, that.getCustomerReference()) &&
            status.equals(that.getStatus()) &&
            message.equals(that.getMessage()) &&
            fee.equals(that.getFee()));
    }
}
