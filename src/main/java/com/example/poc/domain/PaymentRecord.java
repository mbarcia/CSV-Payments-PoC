package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvNumber;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class PaymentRecord extends BasePersistable implements Serializable {
    @NonNull
    @CsvBindByName(column = "ID")
    private String csvId;

    @NonNull
    @CsvBindByName(column = "Recipient")
    private String recipient;

    @NonNull
    @CsvBindByName(column = "Amount")
    @CsvNumber("#,###.00")
    private BigDecimal amount;

    @NonNull
    @CsvBindByName(column = "Currency")
    private Currency currency;

    @CsvIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    private CsvPaymentsFile csvPaymentsFile;

    @CsvIgnore
    @OneToOne(
            cascade = CascadeType.ALL,
            mappedBy = "record"
    )
    private AckPaymentSent ackPaymentSent;

    @Override
    public String toString() {
        return "PaymentRecord{" +
                "id='" + csvId + '\'' +
                ", recipient='" + recipient + '\'' +
                ", amount=" + amount +
                ", currency=" + currency +
                ", file=" + csvPaymentsFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentRecord that = (PaymentRecord) o;
        return Objects.equals(getId(), that.getId()) &&
                getCsvId().equals(that.getCsvId()) &&
                getRecipient().equals(that.getRecipient()) &&
                getAmount().equals(that.getAmount()) &&
                getCsvPaymentsFile().equals(that.getCsvPaymentsFile()) &&
                getCurrency().equals(that.getCurrency()) &&
                Objects.equals(this.ackPaymentSent, that.getAckPaymentSent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCsvId(), getRecipient(), getAmount(), getCurrency());
    }
}
