package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentRecordOutputBean;
import com.example.poc.service.CsvPaymentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
public class UnparseRecordCommand extends BaseCommand<PaymentRecord, PaymentRecordOutputBean> {
    @Autowired
    CsvPaymentsService service;

    public PaymentRecordOutputBean execute(PaymentRecord paymentRecord) {
        PaymentRecordOutputBean paymentRecordOutputBean = new PaymentRecordOutputBean();
        BeanUtils.copyProperties(paymentRecord, paymentRecordOutputBean);
        AckPaymentSent ackPaymentSent = paymentRecord.getAckPaymentSent();

        // Check ackPaymentSent and paymentStatus data is hydrated from the DB
        // Currently, ackPayment sent will be loaded, but paymentStatus will not
        if (ackPaymentSent == null || ackPaymentSent.getPaymentStatus() == null) {
            Optional<AckPaymentSent> ackPaymentSentOptional = service.findAckPaymentSentByRecord(paymentRecord);
            if (ackPaymentSentOptional.isPresent()) {
                ackPaymentSent = ackPaymentSentOptional.get();
            } else {
                Logger logger = LoggerFactory.getLogger(this.getClass());
                logger.warn(String.format("No AckPaymentSent found for record %s", paymentRecord.getId()));
            }
        }

        assert ackPaymentSent != null;
        BeanUtils.copyProperties(ackPaymentSent, paymentRecordOutputBean);
        BeanUtils.copyProperties(ackPaymentSent.getPaymentStatus(), paymentRecordOutputBean);

        return paymentRecordOutputBean;
    }
}
