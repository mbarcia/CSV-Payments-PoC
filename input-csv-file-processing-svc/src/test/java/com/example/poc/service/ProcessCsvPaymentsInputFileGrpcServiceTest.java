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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.dto.PaymentRecordDto;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import io.smallrye.mutiny.Multi;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessCsvPaymentsInputFileGrpcServiceTest {

  private ProcessCsvPaymentsInputFileGrpcService service;

  private ProcessCsvPaymentsInputFileReactiveService domainService;
  private CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;
  private PaymentRecordMapper paymentRecordMapper;

  @BeforeEach
  void setUp() {
    domainService = mock(ProcessCsvPaymentsInputFileReactiveService.class);
    csvPaymentsInputFileMapper = mock(CsvPaymentsInputFileMapper.class);
    paymentRecordMapper = mock(PaymentRecordMapper.class);

    service = new ProcessCsvPaymentsInputFileGrpcService();
    // manual "injection"
    service.domainService = domainService;
    service.csvPaymentsInputFileMapper = csvPaymentsInputFileMapper;
    service.paymentRecordMapper = paymentRecordMapper;
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

    ArgumentCaptor<com.example.poc.common.dto.PaymentRecordDto> dtoCaptor =
        ArgumentCaptor.forClass(com.example.poc.common.dto.PaymentRecordDto.class);
    verify(paymentRecordMapper, times(2)).toGrpc(dtoCaptor.capture());

    // ensure domain service was called with the mapped input
    verify(domainService).process(domainIn);
  }
}
