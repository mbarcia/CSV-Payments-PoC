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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.dto.PaymentRecordDto;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsInputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentRecordMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ProcessCsvPaymentsInputFileGrpcServiceTest {

  private ProcessCsvPaymentsInputFileGrpcService service;

  private ProcessCsvPaymentsInputReactiveService domainService;
  private CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;
  private PaymentRecordMapper paymentRecordMapper;
  private PersistenceManager persistenceManager;

  @BeforeEach
  void setUp() {
    domainService = Mockito.mock(ProcessCsvPaymentsInputReactiveService.class);
    csvPaymentsInputFileMapper = mock(CsvPaymentsInputFileMapper.class);
    paymentRecordMapper = mock(PaymentRecordMapper.class);
    persistenceManager = mock(PersistenceManager.class);

    service = new ProcessCsvPaymentsInputFileGrpcService();
    // manual "injection"
    service.domainService = domainService;
    service.csvPaymentsInputFileMapper = csvPaymentsInputFileMapper;
    service.paymentRecordMapper = paymentRecordMapper;
    service.persistenceManager = persistenceManager;
  }

  @Test
  void remoteProcess_shouldMapAndDelegateToDomainService() {
    // given
    InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcRequest =
        InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().setFilepath("test.csv").build();

    CsvPaymentsInputFile domainIn = new CsvPaymentsInputFile();
    PaymentRecord domainOut1 = new PaymentRecord();
    PaymentRecord domainOut2 = new PaymentRecord();

    InputCsvFileProcessingSvc.PaymentRecord grpcOut1 =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().setId("1").build();
    InputCsvFileProcessingSvc.PaymentRecord grpcOut2 =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().setId("2").build();

    // stubbing mappers
    PaymentRecordDto dto1 =
        PaymentRecordDto.builder()
            .id(UUID.randomUUID())
            .csvId(String.valueOf(UUID.randomUUID()))
            .recipient("Alice")
            .amount(BigDecimal.TEN)
            .currency(Currency.getInstance("USD"))
            .csvPaymentsInputFilePath(Path.of("file1.csv"))
            .build();

    PaymentRecordDto dto2 =
        PaymentRecordDto.builder()
            .id(UUID.randomUUID())
            .csvId(String.valueOf(UUID.randomUUID()))
            .recipient("Bob")
            .amount(BigDecimal.ONE)
            .currency(Currency.getInstance("EUR"))
            .csvPaymentsInputFilePath(Path.of("file2.csv"))
            .build();

    when(csvPaymentsInputFileMapper.fromGrpc(grpcRequest)).thenReturn(domainIn);
    // Stub the persistence manager to return the same entity (persisted)
    when(persistenceManager.persist(domainIn)).thenReturn(Uni.createFrom().item(domainIn));
    when(domainService.process(domainIn))
        .thenReturn(Multi.createFrom().items(domainOut1, domainOut2));
    when(paymentRecordMapper.toDto(domainOut1)).thenReturn(dto1);
    when(paymentRecordMapper.toDto(domainOut2)).thenReturn(dto2);
    when(paymentRecordMapper.toGrpc((PaymentRecordDto) any())).thenReturn(grpcOut1, grpcOut2);

    // when
    List<InputCsvFileProcessingSvc.PaymentRecord> result =
        service.remoteProcess(grpcRequest).collect().asList().await().indefinitely();

    // then
    assertThat(result).containsExactly(grpcOut1, grpcOut2);

    // verify mapping calls
    verify(csvPaymentsInputFileMapper).fromGrpc(grpcRequest);
    verify(persistenceManager).persist(domainIn);

    ArgumentCaptor<io.github.mbarcia.csv.common.dto.PaymentRecordDto> dtoCaptor =
        ArgumentCaptor.forClass(io.github.mbarcia.csv.common.dto.PaymentRecordDto.class);
    verify(paymentRecordMapper, times(2)).toGrpc(dtoCaptor.capture());

    // ensure domain service was called with the mapped input
    verify(domainService).process(domainIn);
  }
}
