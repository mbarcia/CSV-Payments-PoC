package poc.csvrecord;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecord {
    private String id;
    private String name;
    private BigDecimal amount;
    private String filepath;

    public PaymentRecord setFilepath(String filepath) {
        this.filepath = filepath;
        return this;
    }

    @Override
    public String toString() {
        return "PaymentRecord{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", filepath=" + filepath +
                '}';
    }
}
