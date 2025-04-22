package com.example.poc;

import com.example.poc.domain.BaseEntity;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvNumber;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import static java.text.MessageFormat.format;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class PaymentRecord extends BaseEntity implements Serializable {
    @NonNull
    @CsvBindByName(column = "ID")
    private String csvId;

    @NonNull
    @CsvBindByName(column = "Recipient")
    private String recipient;

    // en-UK locale to match the format of the csv input files
    @NonNull
    @CsvBindByName(column = "Amount", locale = "en-UK")
    @CsvNumber("#,###.00")
    private BigDecimal amount;

    @NonNull
    @CsvBindByName(column = "Currency")
    private Currency currency;

    @CsvIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    private CsvPaymentsInputFile csvPaymentsInputFile;

    @CsvIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    private CsvPaymentsOutputFile csvPaymentsOutputFile;

    @Override
    public String toString() {
        return format("PaymentRecord'{'id=''{0}'', recipient=''{1}'', amount={2}, currency={3}, file={4}'}'",
                csvId,
                recipient,
                NumberFormat.getCurrencyInstance(Locale.UK).format(amount),
                currency,
                csvPaymentsInputFile);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentRecord that = (PaymentRecord) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCsvId(), getRecipient(), getAmount(), getCurrency());
    }

}
