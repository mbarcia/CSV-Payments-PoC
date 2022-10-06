package poc.csvrecord;

import poc.Command;

public class SendPaymentCommand implements Command<PaymentRecord> {
    @Override
    public void execute(PaymentRecord processableObj) {
        // call the API
        System.out.printf("Calling API for the bean %s\n", processableObj.getBean().toString());
    }
}
