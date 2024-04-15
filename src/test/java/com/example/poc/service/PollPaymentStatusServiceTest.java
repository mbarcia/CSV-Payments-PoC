package com.example.poc.service;

import com.example.poc.command.PollPaymentStatusCommand;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.repository.AckPaymentSentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PollPaymentStatusServiceTest {

    @Mock
    AckPaymentSentRepository ackPaymentSentRepository;

    @Mock
    PollPaymentStatusCommand pollPaymentStatusCommand;

    @InjectMocks
    PollPaymentStatusService pollPaymentStatusService;

    @Test
    void constructorTest() {
        assertNotNull(pollPaymentStatusService.getRepository());
        assertNotNull(pollPaymentStatusService.getCommand());
    }

    @Test
    void processTest() {
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        pollPaymentStatusService.process(ackPaymentSent);
        verify(ackPaymentSentRepository).save(ackPaymentSent);
        verify(pollPaymentStatusCommand).execute(ackPaymentSent);
    }

    @Test
    void printTest() {
        pollPaymentStatusService.print();
        verify(ackPaymentSentRepository).findAll();
    }
}
