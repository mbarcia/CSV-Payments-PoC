package com.example.poc.command;

import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentRecordOutputBean;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
@Component
public class UnparseRecordCommand extends BaseCommand<PaymentRecord, PaymentRecordOutputBean> {

    public PaymentRecordOutputBean execute(PaymentRecord paymentRecord) {
        super.execute(paymentRecord);

        PaymentRecordOutputBean paymentRecordOutputBean = new PaymentRecordOutputBean();
        BeanUtils.copyProperties(paymentRecord, paymentRecordOutputBean);

        BeanUtils.copyProperties(paymentRecord.getAckPaymentSent(), paymentRecordOutputBean);
        BeanUtils.copyProperties(paymentRecord.getAckPaymentSent().getPaymentStatus(), paymentRecordOutputBean);

        return paymentRecordOutputBean;
    }
}
