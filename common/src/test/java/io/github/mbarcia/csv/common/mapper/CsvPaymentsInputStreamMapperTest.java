/*
 * Copyright (c) 2023-2025 Mariano Barcia
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

package io.github.mbarcia.csv.common.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbarcia.csv.common.domain.CsvPaymentsInputStream;
import io.github.mbarcia.csv.common.dto.CsvPaymentsInputStreamDto;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvPaymentsInputStreamMapperTest {

    private CsvPaymentsInputStreamMapper mapper;

    @BeforeEach
    void setUp() {
        // Create CsvPaymentsInputStreamMapperImpl
        mapper = new CsvPaymentsInputStreamMapperImpl();
    }

    @Test
    void testDomainToDto() {
        // Given
        CsvPaymentsInputStream domain =
                new CsvPaymentsInputStream(
                        new ByteArrayInputStream("test".getBytes()), "test-source");

        // When
        CsvPaymentsInputStreamDto dto = mapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals(domain.getSource(), dto.getSource());
    }

    @Test
    void testDtoToDomain() {
        // Given
        CsvPaymentsInputStreamDto dto =
                CsvPaymentsInputStreamDto.builder().source("test-source").build();

        // When
        CsvPaymentsInputStream domain = mapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(dto.getSource(), domain.getSource());
    }

    @Test
    void testDtoToGrpc() {
        // Given
        CsvPaymentsInputStreamDto dto =
                CsvPaymentsInputStreamDto.builder().source("test-source").build();

        // When
        InputCsvFileProcessingSvc.CsvPaymentsInputStream grpc = mapper.toGrpc(dto);

        // Then
        assertNotNull(grpc);
        assertEquals(dto.getSource(), grpc.getSource());
    }

    @Test
    void testGrpcToDto() {
        // Given
        InputCsvFileProcessingSvc.CsvPaymentsInputStream grpc =
                InputCsvFileProcessingSvc.CsvPaymentsInputStream.newBuilder()
                        .setSource("test-source")
                        .build();

        // When
        CsvPaymentsInputStreamDto dto = mapper.fromGrpc(grpc);

        // Then
        assertNotNull(dto);
        assertEquals("test-source", dto.getSource());
    }

    @Test
    void testDomainToGrpc() {
        // Given
        CsvPaymentsInputStream domain =
                new CsvPaymentsInputStream(
                        new ByteArrayInputStream("test".getBytes()), "test-source");

        // When
        InputCsvFileProcessingSvc.CsvPaymentsInputStream grpc = mapper.toDtoToGrpc(domain);

        // Then
        assertNotNull(grpc);
        assertEquals(domain.getSource(), grpc.getSource());
    }

    @Test
    void testGrpcToDomain() {
        // Given
        InputCsvFileProcessingSvc.CsvPaymentsInputStream grpc =
                InputCsvFileProcessingSvc.CsvPaymentsInputStream.newBuilder()
                        .setSource("test-source")
                        .build();

        // When
        CsvPaymentsInputStream domain = mapper.fromGrpcFromDto(grpc);

        // Then
        assertNotNull(domain);
        assertEquals("test-source", domain.getSource());
    }

    @Test
    void testSerializeDeserialize() throws Exception {
        // Build DTO
        CsvPaymentsInputStreamDto dto =
                CsvPaymentsInputStreamDto.builder().source("test-source").build();

        ObjectMapper mapper = new ObjectMapper();

        // Serialize to JSON
        String json = mapper.writeValueAsString(dto);

        // Deserialize back
        CsvPaymentsInputStreamDto deserialized =
                mapper.readValue(json, CsvPaymentsInputStreamDto.class);

        // Assert equality
        assertEquals(dto, deserialized);
    }
}
