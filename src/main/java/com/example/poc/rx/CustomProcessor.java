package com.example.poc.rx;

import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

public class CustomProcessor extends SubmissionPublisher<PaymentOutput> implements Flow.Processor<PaymentRecord, PaymentOutput> {

    private Flow.Subscription subscription;
    private final Function<PaymentRecord, PaymentOutput> function;
    public CustomProcessor(Function<PaymentRecord, PaymentOutput> function) {
        super();
        this.function = function;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(PaymentRecord std) {
        submit(function.apply(std));
        subscription.request(1);
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("Done");
    }

}
