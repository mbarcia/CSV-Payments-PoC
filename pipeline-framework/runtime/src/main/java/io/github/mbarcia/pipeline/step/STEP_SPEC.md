Step - Not needed anymore. Replaced by Configurable and DeadLetterQueue interfaces. 

Configurable - Has default methods for everything related to config
DeadLetterQueue - Just a method

ConfigurableStep class - Implements Configurable and DeadLetterQueue
It holds the config object, pretty much.

Top-level functional interfaces
OneToOne - Uni<O> apply(Uni<I> item)
OneToMany - Multi<O> applyExpansion(Uni<I> item)
ManyToMany - Multi<O> applyMulti(Multi<I> stream)
ManyToOne - Uni<O> applyReduction(Multi<I> stream)

step/blocking package - Don't care about it for now. Too complex.

step/future package - Same: don't care about it for now. Too complex.

So we had this concrete class before:

```java
public class ProcessPaymentStatusStep 
        extends ConfigurableStep 
        implements OneToOne<PaymentsProcessingSvc.PaymentStatus, PaymentStatusSvc.PaymentOutput> {

    @Inject
    @GrpcClient("process-payment-status")
    MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub processPaymentStatusService;

    @Override
    public Uni<PaymentStatusSvc.PaymentOutput> applyAsyncUni(PaymentsProcessingSvc.PaymentStatus status) {
        return processPaymentStatusService.remoteProcess(status);
    }
}
```

and I want to make it generic, StepOneToOne<I, O>

```java
import io.github.mbarcia.pipeline.service.ReactiveService;

public class StepOneToOne<I, O>
        extends ConfigurableStep
        implements OneToOne<I, O> {

    // processPaymentStatusService --> injected field with GrpcClient annotation;
    public abstract <grpc-client-stub-type> getService();

    @Override

    public Uni<O> apply(Uni<I> status) {
        return getService().remoteProcess(status);
    }
}
```

