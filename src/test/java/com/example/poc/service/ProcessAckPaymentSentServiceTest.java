package com.example.poc.service;

import com.example.poc.command.ProcessAckPaymentSentCommand;
import com.example.poc.repository.AckPaymentSentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProcessAckPaymentSentServiceTest {

    @Mock
    private AckPaymentSentRepository repository;

    @Mock
    private ProcessAckPaymentSentCommand command;

    private ProcessAckPaymentSentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ProcessAckPaymentSentService(repository, command);
    }

    @Test
    void shouldCreateServiceWithDependencies() {
        // Assert
        assertNotNull(service);
    }

    @Test
    void shouldExtendBaseServiceWithAudit() {
        // Assert
        assertInstanceOf(BaseServiceWithAudit.class, service);
    }
}
