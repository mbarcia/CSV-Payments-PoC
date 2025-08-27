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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.repository.AckPaymentSentRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class PersistAckPaymentSentReactiveServiceTest {

  @Mock private AckPaymentSentRepository repository;

  private PersistAckPaymentSentReactiveService persistAckPaymentSentReactiveService;
  private AckPaymentSent testAck;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    persistAckPaymentSentReactiveService = new PersistAckPaymentSentReactiveService(repository);
    testAck = new AckPaymentSent(UUID.randomUUID());
    testAck.setId(UUID.randomUUID());
    testAck.setStatus(1L);
    testAck.setMessage("Processed");

    // Mock the repository to return the same object
    when(repository.persist(any(AckPaymentSent.class))).thenReturn(Uni.createFrom().item(testAck));
  }

  @Test
  void testProcess() {
    // Mock static methods
    try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
      Logger mockLogger = mock(Logger.class);
      try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
        loggerFactoryMock
            .when(() -> LoggerFactory.getLogger(any(Class.class)))
            .thenReturn(mockLogger);

        // Execute the method under test
        Uni<AckPaymentSent> result = persistAckPaymentSentReactiveService.process(testAck);

        // Verify the result
        UniAssertSubscriber<AckPaymentSent> subscriber =
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(testAck);

        // Verify repository interaction
        verify(repository).persist(testAck);

        // Verify MDC interactions
        mdcMock.verify(
            () -> MDC.put("serviceId", PersistAckPaymentSentReactiveService.class.toString()));
        mdcMock.verify(MDC::clear);

        // Verify logger interaction
        verify(mockLogger).info("Persisted entity {}", testAck);
      }
    }
  }

  @Test
  void testGetRepository() {
    // Test that getRepository returns the correct repository
    PanacheRepository<AckPaymentSent> repo = persistAckPaymentSentReactiveService.getRepository();
    // We can't directly compare the wrapped repository, so we just check it's not null
    assertThat(repo).isNotNull();
  }

  @Test
  void testConstructorWithRepository() {
    // Test that the constructor with repository works
    PersistAckPaymentSentReactiveService service =
        new PersistAckPaymentSentReactiveService(repository);
    assertThat(service).isNotNull();
  }
}
