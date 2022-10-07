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
    @JoinColumn(name = "file_id")
    private CSVPaymentsFile file;
    private int apiStatus; // READ, SENT, FAILED_TO_SEND, ACCEPTED, REJECTED

    public PaymentRecord setFilepath(CSVPaymentsFile file) {
        this.file = file;
        return this;
    }

    @Override
    public String toString() {
        return "PaymentRecord{" +
                "id='" + csvId + '\'' +
                ", recipient='" + recipient + '\'' +
                ", amount=" + amount +
                ", currency=" + currency +
                ", file=" + file.getFilepath() +
                '}';
    }
}
