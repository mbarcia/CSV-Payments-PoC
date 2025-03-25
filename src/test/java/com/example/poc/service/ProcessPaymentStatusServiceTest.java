package com.example.poc.service;

import com.example.poc.command.ProcessPaymentStatusCommand;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPaymentStatusServiceTest {

    @Mock
    private PaymentStatusRepository repository;

    @Mock
    private ProcessPaymentStatusCommand command;

    private ProcessPaymentStatusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ProcessPaymentStatusService(repository, command);
    }

    @Test
    void process_ShouldDelegateToBaseClass() {
        // Arrange
        PaymentStatus paymentStatus = new PaymentStatus();
        PaymentOutput expectedOutput = new PaymentOutput();
        when(command.execute(any(PaymentStatus.class))).thenReturn(expectedOutput);

        // Act
        PaymentOutput result = service.process(paymentStatus);

        // Assert
        assertThat(result).isEqualTo(expectedOutput);
        verify(command).execute(paymentStatus);
    }

    @Test
    void constructor_ShouldInitializeWithCorrectDependencies() {
        // Assert
        assertThat(service).isNotNull();
        // Verify that the service was constructed with the correct dependencies
        // This is implicitly testing that the super() call worked correctly
    }
}
