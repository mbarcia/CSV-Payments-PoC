package com.example.poc.domain;

import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

import static java.text.MessageFormat.format;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@NoArgsConstructor
public class AckPaymentSent extends BaseEntity implements Serializable {
    @NonNull
    private String conversationID;

    private Long status;
    private String message;

    @Override
    public String toString() {
        return format("AckPaymentSent'{'id=''{0}'', conversationID=''{1}'', status={2}, message={3}'}'", getId(), conversationID, status, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AckPaymentSent that = (AckPaymentSent) o;
        return this.getId() != null && this.getId().equals(that.getId());
    }
}
