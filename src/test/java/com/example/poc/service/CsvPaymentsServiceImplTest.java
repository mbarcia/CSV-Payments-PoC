package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CsvPaymentsServiceImplTest {

    @InjectMocks
    private CsvPaymentsServiceImpl csvPaymentsService;

    @Mock
    private CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Mock
    private CsvFolderRepository csvFolderRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private AckPaymentSentRepository ackPaymentSentRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void persistRecord() {
        PaymentRecord paymentRecord = getPaymentRecordToPersist();
        when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(paymentRecord);
        assertEquals(csvPaymentsService.persistRecord(paymentRecord), paymentRecord);
        verify(paymentRecordRepository, times(1)).save(any(PaymentRecord.class));
        verifyNoMoreInteractions(paymentRecordRepository);
    }

    private PaymentRecord getPaymentRecordToPersist() {
        return new PaymentRecord("1", "Black Adam", new BigDecimal("420.69"), Currency.getInstance("USD"));
    }

    @Test
    void persistPaymentStatus() {
        PaymentStatus paymentStatus = getPaymentStatusToPersist();
        when(paymentStatusRepository.save(any(PaymentStatus.class))).thenReturn(paymentStatus);
        assertEquals(csvPaymentsService.persistPaymentStatus(paymentStatus), paymentStatus);
        verify(paymentStatusRepository, times(1)).save(any(PaymentStatus.class));
        verifyNoMoreInteractions(paymentStatusRepository);
    }

    private PaymentStatus getPaymentStatusToPersist() {
        return new PaymentStatus("test-10101");
    }

    @Test
    void persistFolder() {
        CsvFolder f = getFolderToPersist();
        when(csvFolderRepository.save(any(CsvFolder.class))).thenReturn(f);
        assertEquals(csvPaymentsService.persistFolder(f), f);
        verify(csvFolderRepository, times(1)).save(any(CsvFolder.class));
        verifyNoMoreInteractions(csvFolderRepository);
    }

    private CsvFolder getFolderToPersist() {
        return new CsvFolder("csv/");
    }

    @Test
    void persistFile() {
        CsvPaymentsFile f = getFileToPersist();
        when(csvPaymentsFileRepository.save(any(CsvPaymentsFile.class))).thenReturn(f);
        assertEquals(csvPaymentsService.persistFile(f), f);
        verify(csvPaymentsFileRepository, times(1)).save(any(CsvPaymentsFile.class));
        verifyNoMoreInteractions(csvPaymentsFileRepository);
    }

    private CsvPaymentsFile getFileToPersist() {
        return new CsvPaymentsFile(new File("test.csv"));
    }

    @Test
    void persistAckPaymentSent() {
        AckPaymentSent ackPaymentSentToPersist = getAckPaymentSentToPersist();
        when(ackPaymentSentRepository.save(any(AckPaymentSent.class))).thenReturn(ackPaymentSentToPersist);
        assertEquals(csvPaymentsService.persistAckPaymentSent(ackPaymentSentToPersist), ackPaymentSentToPersist);
        verify(ackPaymentSentRepository, times(1)).save(any(AckPaymentSent.class));
        verifyNoMoreInteractions(ackPaymentSentRepository);    }

    private AckPaymentSent getAckPaymentSentToPersist() {
        return new AckPaymentSent("nada");
    }
}