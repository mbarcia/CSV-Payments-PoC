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

package io.github.mbarcia.csv.common.mapper;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.csv.common.domain.CsvFolder;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvFolderMapperTest {

    private CsvFolderMapper mapper;
    private CommonConverters commonConverters;

    @BeforeEach
    void setUp() {
        commonConverters = new CommonConverters();

        // Create CsvFolderMapperImpl and set its dependencies
        CsvFolderMapperImpl csvFolderMapperImpl = new CsvFolderMapperImpl();
        try {
            java.lang.reflect.Field commonConvertersField =
                    CsvFolderMapperImpl.class.getDeclaredField("commonConverters");
            commonConvertersField.setAccessible(true);
            commonConvertersField.set(csvFolderMapperImpl, commonConverters);

            mapper = csvFolderMapperImpl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set CsvFolderMapper dependencies", e);
        }
    }

    @Test
    void testDomainToGrpc() {
        // Given
        CsvFolder domain = new CsvFolder();
        domain.setFolderPath(Path.of("/test/folder"));

        // When
        InputCsvFileProcessingSvc.CsvFolder grpc = mapper.toGrpc(domain);

        // Then
        assertNotNull(grpc);
        assertEquals(domain.getFolderPath().toString(), grpc.getFolderPath());
    }

    @Test
    void testGrpcToDomain() {
        // Given
        InputCsvFileProcessingSvc.CsvFolder grpc =
                InputCsvFileProcessingSvc.CsvFolder.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setFolderPath("/test/folder")
                        .build();

        // When
        CsvFolder domain = mapper.fromGrpc(grpc);

        // Then
        assertNotNull(domain);
        assertEquals(Path.of("/test/folder"), domain.getFolderPath());
    }

    // @Test
    // void testSerializeDeserialize() throws Exception {
    //   // Build a simple DTO-like object for testing
    //   InputCsvFileProcessingSvc.CsvFolder.Builder builder =
    //       InputCsvFileProcessingSvc.CsvFolder.newBuilder()
    //           .setFolderPath("/test/folder");
    //
    //   InputCsvFileProcessingSvc.CsvFolder folder = builder.build();

    //   ObjectMapper mapper = new ObjectMapper();
    //
    //   // Serialize to JSON
    //   String json = mapper.writeValueAsString(folder);

    //   // Deserialize back
    //   InputCsvFileProcessingSvc.CsvFolder deserialized = mapper.readValue(json,
    // InputCsvFileProcessingSvc.CsvFolder.class);

    //   // Assert equality
    //   assertEquals(folder, deserialized);
    // }
}
