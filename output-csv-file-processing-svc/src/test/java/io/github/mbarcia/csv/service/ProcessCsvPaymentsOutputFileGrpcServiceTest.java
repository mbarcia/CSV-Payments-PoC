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

package io.github.mbarcia.csv.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("unchecked")
class ProcessCsvPaymentsOutputFileGrpcServiceTest {

    @InjectMocks ProcessCsvPaymentsOutputFileGrpcService grpcService;

    @Mock ProcessCsvPaymentsOutputFileReactiveService domainService;

    @Mock CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Mock PaymentOutputMapper paymentOutputMapper;

    @Mock PersistenceManager persistenceManager;

    private PaymentStatusSvc.PaymentOutput grpcPaymentOutput;
    private PaymentOutput domainPaymentOutput;
    private CsvPaymentsOutputFile domainOutputFile;
    private OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        grpcPaymentOutput =
                PaymentStatusSvc.PaymentOutput.newBuilder()
                        .setCsvId(UUID.randomUUID().toString())
                        .setRecipient("John Doe")
                        .setAmount(new BigDecimal("100.00").toPlainString())
                        .setCurrency(Currency.getInstance("USD").getCurrencyCode())
                        .setConversationId(UUID.randomUUID().toString())
                        .setStatus(1L)
                        .setMessage("Success")
                        .setFee(new BigDecimal("1.50").toPlainString())
                        .build();

        domainPaymentOutput =
                new PaymentOutput(
                        new PaymentStatus(),
                        UUID.randomUUID().toString(),
                        "John Doe",
                        new BigDecimal("100.00"),
                        Currency.getInstance("USD"),
                        UUID.randomUUID(),
                        1L,
                        "Success",
                        new BigDecimal("1.50"));

        domainOutputFile = new CsvPaymentsOutputFile(Path.of("/tmp/output.csv"));

        grpcOutputFile =
                OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder()
                        .setFilepath("/tmp/output.csv")
                        .setCsvFolderPath("/tmp/")
                        .build();
    }

    @Test
    void remoteProcess() {
        // Given
        Multi<PaymentStatusSvc.PaymentOutput> grpcStream =
                Multi.createFrom().item(grpcPaymentOutput);

        when(paymentOutputMapper.fromGrpc(any(PaymentStatusSvc.PaymentOutput.class)))
                .thenReturn(domainPaymentOutput);
        // Stub the persistence manager to return the same entity (persisted)
        when(persistenceManager.persist(domainPaymentOutput))
                .thenReturn(Uni.createFrom().item(domainPaymentOutput));
        when(domainService.process(any(Multi.class)))
                .thenAnswer(
                        invocation -> {
                            Multi<PaymentOutput> input = invocation.getArgument(0);
                            return input.collect()
                                    .asList()
                                    .onItem()
                                    .transform(_ -> domainOutputFile);
                        });
        when(csvPaymentsOutputFileMapper.toGrpc(any(CsvPaymentsOutputFile.class)))
                .thenReturn(grpcOutputFile);

        // When
        Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> resultUni =
                grpcService.remoteProcess(grpcStream);

        // Then
        UniAssertSubscriber<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem().assertItem(grpcOutputFile);
    }

    @Test
    void testRemoteProcess_ShouldInvokeMappers() {
        // Given
        Multi<PaymentStatusSvc.PaymentOutput> grpcStream =
                Multi.createFrom().item(grpcPaymentOutput);

        when(paymentOutputMapper.fromGrpc(grpcPaymentOutput)).thenReturn(domainPaymentOutput);
        // Stub the persistence manager to return the same entity (persisted)
        when(persistenceManager.persist(domainPaymentOutput))
                .thenReturn(Uni.createFrom().item(domainPaymentOutput));
        when(domainService.process(any(Multi.class)))
                .thenAnswer(
                        invocation -> {
                            Multi<PaymentOutput> input = invocation.getArgument(0);
                            return input.collect()
                                    .asList()
                                    .onItem()
                                    .transform(_ -> domainOutputFile);
                        });
        when(csvPaymentsOutputFileMapper.toGrpc(domainOutputFile)).thenReturn(grpcOutputFile);

        // When
        grpcService.remoteProcess(grpcStream).await().indefinitely();

        // Then
        verify(paymentOutputMapper).fromGrpc(grpcPaymentOutput);
        verify(csvPaymentsOutputFileMapper).toGrpc(domainOutputFile);
    }
}
