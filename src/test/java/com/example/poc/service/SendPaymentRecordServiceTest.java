package com.example.poc.service;

import com.example.poc.command.SendPaymentRecordCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendPaymentRecordServiceTest {

    @Mock
    private PaymentRecordRepository repository;

    @Mock
    private SendPaymentRecordCommand command;

    private SendPaymentRecordService service;

    @BeforeEach
    void setUp() {
        service = new SendPaymentRecordService(repository, command);
    }

    @Test
    void process_shouldSaveToRepositoryAndExecuteCommand() {
        // Given
        PaymentRecord paymentRecord = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        AckPaymentSent expectedAck = new AckPaymentSent();

        doNothing().when(repository).persist(any(PaymentRecord.class));
        when(command.execute(paymentRecord)).thenReturn(expectedAck);

        // When
        AckPaymentSent result = service.process(paymentRecord);

        // Then
        verify(repository).persist(paymentRecord);
        verify(command).execute(paymentRecord);
        assertEquals(expectedAck, result);
    }

    @Test
    void print_shouldCallFindAllOnRepository() {
        // Given
        PaymentRecord record1 = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        PaymentRecord record2 = new PaymentRecord("2", "Aurelio", new BigDecimal("124.50"), Currency.getInstance("USD"));
        when(repository.listAll()).thenReturn(List.of(record1, record2));

        // When
        service.print();

        // Then
        verify(repository).listAll();
    }

    @Test
    void getRepository_shouldReturnRepository() {
        // When
        PaymentRecordRepository result = (PaymentRecordRepository) service.getRepository();

        // Then
        assertNotNull(result);
        assertEquals(repository, result);
    }

    @Test
    void getCommand_shouldReturnCommand() {
        // When
        SendPaymentRecordCommand result = (SendPaymentRecordCommand) service.getCommand();

        // Then
        assertNotNull(result);
        assertEquals(command, result);
    }

    @Test
    void process_shouldPropagateExceptions() {
        // Given
        PaymentRecord paymentRecord = new PaymentRecord();
        RuntimeException expectedException = new RuntimeException("Test exception");

        doThrow(expectedException).when(repository).persist(any(PaymentRecord.class));

        // When/Then
        assertThrows(RuntimeException.class, () -> service.process(paymentRecord));
    }

    @Test
    void process_shouldHandleNullInputGracefully() {
        // When/Then
        assertDoesNotThrow(() -> service.process(null));
    }
}
