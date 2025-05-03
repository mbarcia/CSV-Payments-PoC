package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvNumber;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Entity
@RequiredArgsConstructor
@Getter
@Setter
public class PaymentOutput extends BaseEntity implements Serializable {

    @Setter(AccessLevel.NONE) // Avoids override by MapStruct
    private UUID id = UUID.randomUUID();

    @CsvIgnore
    @Transient
    private PaymentRecord paymentRecord;
    private UUID paymentRecordId;

    @CsvIgnore
    @Transient
    private CsvPaymentsOutputFile csvPaymentsOutputFile;
    private String csvPaymentsOutputFilename;

    // en-UK locale to match the format of the (mock) payment service
    @CsvBindByName(column = "CSV Id") String csvId;
    @CsvBindByName(column = "Recipient") String recipient;
    @CsvBindByName(column = "Amount", locale = "en-UK") @CsvNumber("#,###.00") BigDecimal amount;
    @CsvBindByName(column = "Currency") Currency currency;
    @CsvBindByName(column = "Reference") String conversationId;
    @CsvBindByName(column = "Status") Long status;
    @CsvBindByName(column = "Message") String message;
    @CsvBindByName(column = "Fee", locale = "en-UK") @CsvNumber("#,###.00") BigDecimal fee;

    public PaymentOutput(PaymentRecord paymentRecord, UUID paymentRecordId, CsvPaymentsOutputFile csvPaymentsOutputFile, String csvPaymentsOutputFilename, String csvId, String recipient, BigDecimal amount, Currency currency, String conversationId, Long status, String message, BigDecimal fee) {
        this.paymentRecord = paymentRecord;
        this.paymentRecordId = paymentRecordId;
        this.csvPaymentsOutputFile = csvPaymentsOutputFile;
        this.csvPaymentsOutputFilename = csvPaymentsOutputFilename;
        this.csvId = csvId;
        this.recipient = recipient;
        this.amount = amount;
        this.currency = currency;
        this.conversationId = conversationId;
        this.status = status;
        this.message = message;
        this.fee = fee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentOutput that = (PaymentOutput) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
