# Proposal: Out-of-the-box default Pipeline CLI application

## Overview

I want to provide user of the framework with an out-of-the-box working CLI application solution to his pipeline problem.

I want a wizard-like tool that generates all the bits and bobs for the user to just answer a few questions and sit and enjoy the show.

When in doubt, just follow along the structure of the CSV Payments app (but needs to be generic of course)

## Proposed Solution
The wizard should ask the following questions on a CLI interface, using Mustache.

Name of the application?

Name of the base package (if any)? (i.e. io.github.mbarcia)

New Step: name (or Enter for finish)

Cardinality? 1-1, Expansion (1-many), Reduction (many-1), or side-effect-only 1-1

Input type name? (plural if reduction)

Fields:

Field name? (or Enter for next step)

Field type? (from a list of available types in protbuf)

Output type name? (plural if expansion)
(same fields description for the output type)

At the end, the template engine should generate, first an app directory with:
* parent POM
* docker-compose.yaml
* utility scripts (up and down-docker.sh, up and down-local.sh, exclude render*.sh)
* run-e2e-test.sh
* all the observability stuff
* mvnw and mvnw.cmd
* a common subdir with its POM
* a sub-directory per step, <stepname-svc>
* an orchestrator subdir

## Common

### POM
Look into the app's common pom.xml
Mileage may vary here, as the dependencies will cater to the needs of the CSV
payments app...

### Proto
In the common subdir, under proto,
1 proto definition file per step, with the input type, output type, and a rpc call, taking into account cardinality.
Notice how the names on this definition file will dictate the names of the types and RPC methods later on.
Each proto file should include the proto definition of the previous step. Don't forget the java_package, use the app name for this.

### Entities
Under domain, the entities: `extends BaseEntity implements Serializable` is key here

### DTOs
Under dto, the dtos. The dtos should be identical to the entities, but they do feature a number of differences:
```java
@Value
@Builder
@JsonDeserialize(builder = AckPaymentSentDto.AckPaymentSentDtoBuilder.class)
public class AckPaymentSentDto {
  UUID id;
  UUID conversationId;
  UUID paymentRecordId;
  PaymentRecord paymentRecord;
  String message;
  Long status;

  // Lombok will generate the builder, but Jackson needs to know how to interpret it
  @JsonPOJOBuilder(withPrefix = "")
  public static class AckPaymentSentDtoBuilder {}

```
They do Value/Builder and the JsonDeserialize needs the inner static class as well. Also they define the UUID id.

### Mappers
Under mapper, generate one mapper per step plus the last output type, need to implement Mapper and be annotated with MapStruct 
```java
@Mapper(
    componentModel = "jakarta",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PaymentRecordMapper extends org.pipelineframework.mapper.Mapper<InputCsvFileProcessingSvc.PaymentRecord, PaymentRecordDto, PaymentRecord> {
    ...

```
Needs to include CommonConverters 
Needs to also include the mapper of the previous step here (unless it is the input type of the first step)
Notice the types: Infer them from the proto definitions. If necessary, use the same logic used by the gRPC extension, in the form
grpc-unit-file-name.grpc-rpc-endpoint-name

## Microservice modules

### POM
Look into the microservices' pom.xml.
Mileage may vary here, as the dependencies will cater to the needs of the CSV
payments app...

Using the same root package name, create a sample service name using the 
```java
@PipelineStep(
    order = <step-order>,
    inputType = <input-type>.class,
    outputType = <output-type>.class,
    stepType = <cardinality-type-functional-interface>.class,
    backendType = <GenericGrpcService-adapter-class based on cardinality>.class,
    grpcStub = <<root-package-name>.<app-name>.grpc.Mutiny<proto-rpc-endpoint-name>Grpc>.Mutiny<proto-rpc-endpoint-name>Stub.class,
    grpcImpl = Mutiny<proto-rpc-endpoint-name>Grpc.<proto-rpc-endpoint-name>ImplBase.class,
    inboundMapper = <inbound-mapper-class-name>.class,
    outboundMapper = <outbound-mapper-class-name>.class,
    grpcClient = "<proto-rpc-endpoint-name>",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class <class-name>
    implements <Reactive functional interface according to cardinality><input-type, output-type> {
```

and then, a shell of
`public Multi<output-type> process(input-type input) {
  // TODO implement business logic here
}` but Uni or Multi, according to cardinality

### Dockerfile
Last but not least, the Dockerfile (render/rebuild-dockerfiles.sh can help here).

## Orchestrator

### POM
Look into the orchestrator-svc pom.xml. Should be identical I think, of course minding the group differences.

This should actually be empty!

Did I miss anything?
