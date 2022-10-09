package com.example.poc.biz;

import com.opencsv.bean.CsvBindByName;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

@Entity
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
    @ManyToOne
    private CsvPaymentsFile csvPaymentsFile;

    public CsvPaymentsFile getCsvPaymentsFile() {
        return csvPaymentsFile;
    }

    public void setCsvPaymentsFile(CsvPaymentsFile csvPaymentsFile) {
        this.csvPaymentsFile = csvPaymentsFile;
    }

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
