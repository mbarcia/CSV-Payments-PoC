package com.example.poc;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvNumber;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

@Entity
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Getter
@Setter
public class PaymentOutput implements Serializable {
    @Id
    @CsvIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CsvIgnore
    @OneToOne
    private final PaymentRecord paymentRecord;

    @CsvIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    private final CsvPaymentsOutputFile csvPaymentsOutputFile;

    // en-UK locale to match the format of the (mock) payment service
    @CsvBindByName(column = "CSV Id") final String csvId;
    @CsvBindByName(column = "Recipient") final String recipient;
    @CsvBindByName(column = "Amount", locale = "en-UK") @CsvNumber("#,###.00") final BigDecimal amount;
    @CsvBindByName(column = "Currency") final Currency currency;
    @CsvBindByName(column = "Reference") final String conversationID;
    @CsvBindByName(column = "Status") final Long status;
    @CsvBindByName(column = "Message") final String message;
    @CsvBindByName(column = "Fee", locale = "en-UK") @CsvNumber("#,###.00") final BigDecimal fee;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentOutput that = (PaymentOutput) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
