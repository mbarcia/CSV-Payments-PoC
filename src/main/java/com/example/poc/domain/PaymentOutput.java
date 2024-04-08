package com.example.poc.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Getter
public class PaymentOutput extends BasePersistable {
    @CsvBindByName(column = "CSV Id") final String csvId;
    @CsvBindByName(column = "Recipient") final String recipient;
    @CsvBindByName(column = "Amount") @CsvNumber("#,###.00") final BigDecimal amount;
    @CsvBindByName(column = "Currency") final Currency currency;
    @CsvBindByName(column = "Reference") final String conversationID;
    @CsvBindByName(column = "Status") final Long status;
    @CsvBindByName(column = "Message") final String message;
    @CsvBindByName(column = "Fee") @CsvNumber("#,###.00") final BigDecimal fee;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentOutput that = (PaymentOutput) o;
        return Objects.equals(getId(), that.getId()) &&
                getCsvId().equals(that.getCsvId()) &&
                getRecipient().equals(that.getRecipient()) &&
                getAmount().equals(that.getAmount()) &&
                getCurrency().equals(that.getCurrency()) &&
                getConversationID().equals(that.getConversationID()) &&
                getStatus().equals(that.getStatus()) &&
                Objects.equals(getMessage(), that.getMessage()) &&
                Objects.equals(getFee(), that.getFee());
    }
}
