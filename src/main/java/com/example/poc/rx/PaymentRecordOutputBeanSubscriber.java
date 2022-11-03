package com.example.poc.rx;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class PaymentRecordOutputBeanSubscriber implements Subscriber<PaymentOutput> {
    private final String fileName;
    private Subscription subscription;

    private final Writer writer;

    // TODO
    // StatefulBeanToCsv is NOT thread-safe
    private final StatefulBeanToCsv<PaymentOutput> sbc;
    private int counter = 0;

    public PaymentRecordOutputBeanSubscriber(CsvPaymentsFile file) {
        try {
            fileName = file.getFilepath() + ".reactive.out";
            writer = new FileWriter(fileName);
            sbc = new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
                    .withQuotechar('\'')
                    .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println("onSubscribe for PaymentRecordOutputBeanSubscriber called");

        this.subscription = subscription;
        this.subscription.request(1); //requesting data from publisher
        System.out.println("onSubscribe for PaymentRecordOutputBeanSubscriber requested 1 PaymentOutput");
    }

    @Override
    public void onNext(PaymentOutput paymentOutput) {
        System.out.println("Processing PaymentOutput " + paymentOutput);
        counter++;
        try {
            sbc.write(paymentOutput);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RuntimeException(e);
        }
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable e) {
        try {
            this.writer.close();
        } catch (IOException ex) {
            // ignore anyway
        }
        System.out.println("Some error happened: " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void onComplete() {
        try {
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All Processing Done for " + fileName);
    }

    public int getCounter() {
        return counter;
    }
}