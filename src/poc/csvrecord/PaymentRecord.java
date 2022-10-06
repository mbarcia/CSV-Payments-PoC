package poc.csvrecord;

import poc.Processable;

public class PaymentRecord extends Processable<PaymentRecord> {
    private PaymentRecordBean bean;

    public PaymentRecord(PaymentRecordBean bean) {
        setBean(bean);
    }
    public PaymentRecordBean getBean() {
        return bean;
    }

    public void setBean(PaymentRecordBean bean) {
        this.bean = bean;
    }
}
