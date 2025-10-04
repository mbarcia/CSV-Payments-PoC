# Backend Service Structure

Each backend service implements a specific pipeline step and follows the patterns demonstrated in the CSV Payments reference implementation.

## Service Implementation

Annotate your service class with `@PipelineStep`. Use the `inputGrpcType` and `outputGrpcType` parameters to explicitly specify the gRPC types that should be used in the generated client step interface:

```java
// step-one-svc/src/main/java/com/example/app/stepone/service/ProcessPaymentStep.java
@PipelineStep(
    order = 1,
    inputType = PaymentRecord.class,
    outputType = PaymentStatus.class,
    inputGrpcType = PaymentsProcessingSvc.PaymentRecord.class,
    outputGrpcType = PaymentsProcessingSvc.PaymentStatus.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
    grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
    inboundMapper = PaymentRecordMapper.class,
    outboundMapper = PaymentStatusMapper.class,
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

The `inputGrpcType` and `outputGrpcType` parameters allow you to explicitly specify the gRPC message types that should be used in the generated client step interfaces. When these parameters are provided, the framework will use these exact types in the generated step implementations instead of trying to infer them from the domain types. This gives you more control over the interface contracts and helps avoid issues where the inferred types don't match the expected gRPC service contract.

## Step-Specific Mappers

Mappers that are specific to a step should be in that service's module using MapStruct:

```java
// step-one-svc/src/main/java/com/example/app/stepone/mapper/PaymentRecordMapper.java
@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface PaymentRecordMapper extends Mapper<PaymentRecordGrpc, PaymentRecordDto, PaymentRecord> {

    PaymentRecordMapper INSTANCE = Mappers.getMapper(PaymentRecordMapper.class);

    // Domain ↔ DTO
    @Override
    PaymentRecordDto toDto(PaymentRecord domain);

    @Override
    PaymentRecord fromDto(PaymentRecordDto dto);

    // DTO ↔ gRPC
    @Override
    @Mapping(target = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "currency", qualifiedByName = "currencyToString")
    PaymentRecordGrpc toGrpc(PaymentRecordDto dto);

    @Override
    @Mapping(target = "id", qualifiedByName = "stringToUUID")
    @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
    PaymentRecordDto fromGrpc(PaymentRecordGrpc grpc);
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