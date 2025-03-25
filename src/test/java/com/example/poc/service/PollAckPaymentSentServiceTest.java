package com.example.poc.service;

import com.example.poc.command.PollAckPaymentSentCommand;
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
class PollAckPaymentSentServiceTest {

    @Mock
    AckPaymentSentRepository ackPaymentSentRepository;

    @Mock
    PollAckPaymentSentCommand pollAckPaymentSentCommand;

    @InjectMocks
    PollAckPaymentSentService pollAckPaymentSentService;

    @Test
    void constructorTest() {
        assertNotNull(pollAckPaymentSentService.getRepository());
        assertNotNull(pollAckPaymentSentService.getCommand());
    }

    @Test
    void processTest() {
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        pollAckPaymentSentService.process(ackPaymentSent);
        verify(ackPaymentSentRepository).save(ackPaymentSent);
        verify(pollAckPaymentSentCommand).execute(ackPaymentSent);
    }

    @Test
    void printTest() {
        pollAckPaymentSentService.print();
        verify(ackPaymentSentRepository).findAll();
    }
}
