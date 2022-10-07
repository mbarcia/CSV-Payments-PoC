package poc.csvrecord;

import poc.Command;

public class SendPaymentCommand implements Command<PaymentRecord, PaymentRecord> {
    @Override
    public PaymentRecord execute(PaymentRecord paymentRecord) {
        // call the API

        // return same object so the stream processing can continue
        return paymentRecord;
    }
}
