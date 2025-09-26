# Backend Service Structure

Each backend service implements a specific pipeline step and follows the patterns demonstrated in the CSV Payments reference implementation.

## Service Implementation

Annotate your service class with `@PipelineStep`:

```java
// step-one-svc/src/main/java/com/example/app/stepone/service/ProcessPaymentStep.java
@PipelineStep(
    order = 1,
    inputType = PaymentRecord.class,
    outputType = PaymentStatus.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
    grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
    inboundMapper = PaymentRecordInboundMapper.class,
    outboundMapper = PaymentStatusOutboundMapper.class,
    grpcClient = "process-payment",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
public class ProcessPaymentStep implements StepOneToOne<PaymentRecord, PaymentStatus> {
    
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
        // Implementation here
        return Uni.createFrom().item(/* processed payment status */);
    }
}
```

## Step-Specific Mappers

Mappers that are specific to a step should be in that service's module:

```java
// step-one-svc/src/main/java/com/example/app/stepone/mapper/PaymentRecordInboundMapper.java
@ApplicationScoped
public class PaymentRecordInboundMapper implements InboundMapper<PaymentRecordGrpc, PaymentRecord> {
    
    @Override
    public PaymentRecord fromGrpc(PaymentRecordGrpc grpc) {
        // Convert gRPC to domain
        return new PaymentRecord(/* ... */);
    }
}
```

## Service Architecture Diagram

```mermaid
graph TD
    subgraph "Backend Service"
        A[Service Implementation]
        B[Step-Specific Mappers]
        C[@PipelineStep Annotation]
    end
    
    A --> B
    B --> C
    C --> A
```