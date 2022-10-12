package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

@Entity
@Table(name = "record")
public class PaymentRecord implements Serializable {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Getter
    @CsvBindByName(column = "ID")
    private String csvId;
    @CsvBindByName(column = "Recipient")
    private String recipient;
    @CsvBindByName(column = "Amount")
    @Getter
    private BigDecimal amount;
    @CsvBindByName(column = "Currency")
    @Getter
    private Currency currency;
    @ManyToOne
    private CsvPaymentsFile csvPaymentsFile;

    @OneToOne(mappedBy = "record", cascade = CascadeType.ALL)
    @Getter @Setter
    private SendPayment sendPayment;

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
                ", file=" + csvPaymentsFile.getFilepath() +
                '}';
    }
}
