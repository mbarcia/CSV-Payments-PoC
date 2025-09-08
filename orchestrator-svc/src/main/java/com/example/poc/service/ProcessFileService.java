/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.poc.service;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class ProcessFileService {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessFileService.class);

  public static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  @Inject
  @GrpcClient("process-csv-payments-input-file")
  MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub
      processCsvPaymentsInputFileService;

  @Inject
  @GrpcClient("process-ack-payment-sent")
  MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub
      processAckPaymentSentService;

  @Inject
  @GrpcClient("persist-payment-record")
  MutinyPersistPaymentRecordServiceGrpc.MutinyPersistPaymentRecordServiceStub persistPaymentRecordService;

  @Inject
  @GrpcClient("send-payment-record")
  MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

  @Inject
  @GrpcClient("persist-ack-payment-sent")
  MutinyPersistAckPaymentSentServiceGrpc.MutinyPersistAckPaymentSentServiceStub persistAckPaymentSentService;

  @Inject
  @GrpcClient("process-csv-payments-output-file")
  MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub
      processCsvPaymentsOutputFileService;

  @Inject
  @GrpcClient("process-payment-status")
  MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub
      processPaymentStatusService;

  @Inject CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Inject CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

  @Inject
  ProcessFileServiceConfig config;

    public Uni<CsvPaymentsOutputFile> process(CsvPaymentsInputFile inputFile) {
    LOG.info("🧵 {} running on thread: {}", inputFile, Thread.currentThread());
    
    // Initial input processing
    Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords = processInputFile(inputFile);

    // Full pipeline per-record, all running concurrently
    Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMulti = processPaymentRecords(inputRecords);

    // Final output processing
    return processOutputFile(paymentOutputMulti, inputFile);
  }

  private Multi<InputCsvFileProcessingSvc.PaymentRecord> processInputFile(CsvPaymentsInputFile inputFile) {
    LOG.debug("Attempting to call processCsvPaymentsInputFileService.remoteProcess");
    Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords =
        processCsvPaymentsInputFileService.remoteProcess(
            csvPaymentsInputFileMapper.toGrpc(inputFile))
        .onFailure()
        .invoke(e -> {
          if (e instanceof StatusRuntimeException grpcEx) {
            LOG.error("gRPC error when calling processCsvPaymentsInputFileService: code={0}, description={}, cause={}",
                grpcEx.getStatus().getCode(), 
                grpcEx.getStatus().getDescription(), 
                grpcEx.getCause());
          } else {
            LOG.error("Non-gRPC error when calling processCsvPaymentsInputFileService", e);
          }
        });
    LOG.debug("Successfully called processCsvPaymentsInputFileService.remoteProcess");
    return inputRecords;
  }

  private Uni<CsvPaymentsOutputFile> processOutputFile(Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMulti, CsvPaymentsInputFile inputFile) {
    // Now send final output for entire file
    Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMultiIntermediate =
        paymentOutputMulti
            .collect()
            .asList()
            .onItem()
            .transformToMulti(Multi.createFrom()::iterable);

    LOG.debug("Attempting to call processCsvPaymentsOutputFileService.remoteProcess");
    return processCsvPaymentsOutputFileService
        .remoteProcess(paymentOutputMultiIntermediate)
        .onItem()
        .transform(csvPaymentsOutputFileMapper::fromGrpc)
        .onItem()
        .invoke(result -> LOG.info("✅ Completed processing: {}", result))
        .onFailure()
        .invoke(e -> LOG.error("❌ Processing failed for: {}", inputFile, e));
  }

  private Multi<PaymentStatusSvc.PaymentOutput> processPaymentRecords(Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords) {
    return inputRecords
        .onItem()
        .transformToUni(this::processSingleRecord)
        .merge(config.getConcurrencyLimitRecords()) // Control concurrency across full pipelines
        .onFailure()
        .invoke(e -> LOG.error("Error processing records stream", e));
  }

  private Uni<PaymentStatusSvc.PaymentOutput> processSingleRecord(InputCsvFileProcessingSvc.PaymentRecord record) {
    LOG.debug("Processing record: {}", record);
    return Uni.createFrom()
        .item(record)
        .runSubscriptionOn(VIRTUAL_EXECUTOR)
        .chain(this::executeStep1)
        .chain(this::executeStep2)
        // Example of how to add a new step here:
        // .chain(this::executeNewStep)
        .chain(this::executeStep3);
  }

  // Example of how to add a new step:
  /*
  private Uni<SomeNewType> executeNewStep(PaymentsProcessingSvc.PaymentStatus status) {
    LOG.debug("Executing New Step for status: {}", status);
    return Uni.createFrom()
        .item(status)
        .flatMap(someNewGrpcService::remoteProcess)
        .onFailure(this::isThrottlingError)
        .retry()
        .withBackOff(Duration.ofMillis(config.getInitialRetryDelay()), Duration.ofMillis(config.getInitialRetryDelay() * 2))
        .atMost(config.getMaxRetries())
        .onFailure()
        .transform(this::handleGrpcError)
        .onFailure()
        .invoke(e -> LOG.error("Error in New Step for status: {}", status, e));
  }
  */

  private Uni<PaymentsProcessingSvc.AckPaymentSent> executeStep1(InputCsvFileProcessingSvc.PaymentRecord record) {
    LOG.debug("Executing Step 1: Persist PaymentRecord and Send Payment for record: {}", record);
    return Uni.createFrom()
        .item(record)
        .flatMap(persistPaymentRecordService::remoteProcess)
        .flatMap(sendPaymentRecordService::remoteProcess)
        .onFailure(this::isThrottlingError)
        .retry()
        .withBackOff(Duration.ofMillis(config.getInitialRetryDelay()), Duration.ofMillis(config.getInitialRetryDelay() * 2))
        .atMost(config.getMaxRetries())
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 1 for record: {}", record, e));
  }

  private Uni<PaymentsProcessingSvc.PaymentStatus> executeStep2(PaymentsProcessingSvc.AckPaymentSent ack) {
    LOG.debug("Executing Step 2: Persist and Process Ack for ack: {}", ack);
    return Uni.createFrom()
        .item(ack)
        .flatMap(processAckPaymentSentService::remoteProcess)
        .onFailure(this::isThrottlingError)
        .retry()
        .withBackOff(Duration.ofMillis(config.getInitialRetryDelay()), Duration.ofMillis(config.getInitialRetryDelay() * 2))
        .atMost(config.getMaxRetries())
        .onFailure()
        .transform(this::handleGrpcError)
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 2 for ack: {}", ack, e));
  }

  private Uni<PaymentStatusSvc.PaymentOutput> executeStep3(PaymentsProcessingSvc.PaymentStatus status) {
    LOG.debug("Executing Step 3: Process Status for status: {}", status);
    return Uni.createFrom()
        .item(status)
        .flatMap(processPaymentStatusService::remoteProcess)
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 3 for status: {}", status, e));
  }

  private Uni<PaymentsProcessingSvc.AckPaymentSent> step1PersistAndSendPayment(InputCsvFileProcessingSvc.PaymentRecord record) {
    LOG.debug("Processing record: {}", record);
    return Uni.createFrom()
        .item(record)
        .runSubscriptionOn(VIRTUAL_EXECUTOR)
        .flatMap(persistPaymentRecordService::remoteProcess)
        .flatMap(sendPaymentRecordService::remoteProcess)
        .onFailure(this::isThrottlingError)
        .retry()
        .withBackOff(Duration.ofMillis(config.getInitialRetryDelay()), Duration.ofMillis(config.getInitialRetryDelay() * 2))
        .atMost(config.getMaxRetries())
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 1 for record: {}", record, e));
  }

  private Uni<PaymentsProcessingSvc.PaymentStatus> step2PersistAndProcessAck(PaymentsProcessingSvc.AckPaymentSent ack) {
    LOG.debug("Processing ack: {}", ack);
    return Uni.createFrom()
        .item(ack)
        .runSubscriptionOn(VIRTUAL_EXECUTOR)
        .flatMap(processAckPaymentSentService::remoteProcess)
        .onFailure(this::isThrottlingError)
        .retry()
        .withBackOff(Duration.ofMillis(config.getInitialRetryDelay()), Duration.ofMillis(config.getInitialRetryDelay() * 2))
        .atMost(config.getMaxRetries())
        .onFailure()
        .transform(this::handleGrpcError)
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 2 for ack: {}", ack, e));
  }

  private Uni<PaymentStatusSvc.PaymentOutput> step3ProcessStatus(PaymentsProcessingSvc.PaymentStatus status) {
    LOG.debug("Processing status: {}", status);
    return Uni.createFrom()
        .item(status)
        .runSubscriptionOn(VIRTUAL_EXECUTOR)
        .flatMap(processPaymentStatusService::remoteProcess)
        .onFailure()
        .invoke(e -> LOG.error("Error in Step 3 for status: {}", status, e));
  }

  // Helper predicate for checking gRPC throttling errors
  public boolean isThrottlingError(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException grpcEx) {
      Status.Code code = grpcEx.getStatus().getCode();
      LOG.debug("gRPC error code: {} - message: {}", code, grpcEx.getStatus().getDescription());
      
      // Common gRPC status codes for throttling/resource exhaustion:
      // RESOURCE_EXHAUSTED: The system is out of resources, or the request is rejected by a rate
      // limit.
      // UNAVAILABLE: The service is currently unavailable. This is most likely a transient
      // condition.
      // ABORTED: The operation was aborted, typically due to a concurrency issue like a transaction
      // abort.
      return code == Status.Code.RESOURCE_EXHAUSTED
          || code == Status.Code.UNAVAILABLE
          || code == Status.Code.ABORTED;
    }

    return false;
  }

  // Handle specific gRPC errors, especially the "Invalid gRPC status null" error
  private Throwable handleGrpcError(Throwable throwable) {
    LOG.debug("Handling gRPC error: {}", throwable.getClass().getName(), throwable);
    
    if (throwable instanceof NoStackTraceThrowable noStackTraceEx) {
      String message = noStackTraceEx.getMessage();
      if (message != null && message.contains("Invalid gRPC status null")) {
        LOG.error("Received 'Invalid gRPC status null' error. This typically indicates a timeout or connection issue in Docker Compose. Converting to proper gRPC error for retry logic.");
        // Convert to a proper gRPC error that can be retried
        return new StatusRuntimeException(Status.UNAVAILABLE.withDescription(MessageFormat.format("Service timeout or connection issue: {0}", message)).withCause(throwable));
      }
    } else if (throwable instanceof StatusRuntimeException statusEx) {
      Status.Code code = statusEx.getStatus().getCode();
      String description = statusEx.getStatus().getDescription();
      
      // Log detailed information about the error
      LOG.debug("StatusRuntimeException - Code: {0}, Description: {}, Cause: {}",
          code, description, statusEx.getCause());
      
      // If we get an UNKNOWN error with null description, treat it similarly
      if (code == Status.Code.UNKNOWN && description == null) {
        LOG.error("Received UNKNOWN gRPC error with null description. Converting to UNAVAILABLE for retry logic.");
        return new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Unknown service error").withCause(throwable));
      }
    }
    
    // If it's already a gRPC error or other type, return as-is
    return throwable;
  }
}
