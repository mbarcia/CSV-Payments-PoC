package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@Entity
@Getter
@Setter
@Accessors(chain = true)
public class PaymentRecord implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @CsvBindByName(column = "ID")
    private String csvId;

    @CsvBindByName(column = "Recipient")
    private String recipient;

    @CsvBindByName(column = "Amount")
    private BigDecimal amount;

    @CsvBindByName(column = "Currency")
    private Currency currency;

    @ManyToOne(fetch = FetchType.EAGER)
    private CsvPaymentsFile csvPaymentsFile;

    @OneToOne(
            cascade = CascadeType.ALL,
            mappedBy = "record"
    )
    private AckPaymentSent ackPaymentSent;

    public PaymentRecord setFile(CsvPaymentsFile file) {
        this.csvPaymentsFile = file;
        return this;
    }

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
        return Objects.equals(getId(), that.getId()) && getCsvId().equals(that.getCsvId()) && getRecipient().equals(that.getRecipient()) && getAmount().equals(that.getAmount()) && getCurrency().equals(that.getCurrency());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCsvId(), getRecipient(), getAmount(), getCurrency());
    }
}
