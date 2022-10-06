package poc.csvrecord;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecordBean {
    private String id;
    private String name;
    private BigDecimal amount;
}
