package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentRecordOutputBean;
import com.example.poc.service.CsvPaymentsService;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CsvOutputCommand extends BaseCommand<CsvPaymentsFile, CsvPaymentsFile> {
    @Autowired
    CsvPaymentsService service;

    @Transactional
    public CsvPaymentsFile execute(CsvPaymentsFile aFile) {
        super.execute(aFile);

        List<PaymentRecord> processedFileData = aFile.getRecords();

        try (Writer writer = new FileWriter(aFile.getFilepath() + ".out")) {

            StatefulBeanToCsv<PaymentRecordOutputBean> sbc = new StatefulBeanToCsvBuilder<PaymentRecordOutputBean>(writer)
                    .withQuotechar('\'')
                    .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                    .build();

            List<PaymentRecordOutputBean> result = new ArrayList<>();
            for (PaymentRecord record : processedFileData) {
                PaymentRecordOutputBean bean = new PaymentRecordOutputBean();
                BeanUtils.copyProperties(record, bean);
                Optional<AckPaymentSent> ackPaymentSentOptional = service.findAckPaymentSentByRecord(record);
                ackPaymentSentOptional.ifPresent(ack -> {
                    BeanUtils.copyProperties(ack, bean);
                    BeanUtils.copyProperties(ack.getPaymentStatus(), bean);
                    result.add(bean);
                });
            }

            sbc.write(result);
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            throw new RuntimeException(e);
        }

        return aFile;
    }
}
