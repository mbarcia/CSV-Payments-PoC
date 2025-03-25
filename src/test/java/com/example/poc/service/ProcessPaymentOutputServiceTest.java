package com.example.poc.service;

import com.example.poc.command.ProcessPaymentOutputCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.CsvPaymentsOutputFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentOutputServiceTest {

    @Mock
    private ProcessPaymentOutputCommand command;

    @Mock
    private CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository;

    @Mock
    private CsvPaymentsInputFile mockInputFile;

    @Mock
    private CsvPaymentsOutputFile mockOutputFile;

    @Mock
    private PaymentOutput mockPaymentOutput;

    @Mock
    private PaymentRecord mockPaymentRecord;

    @Mock
    private BufferedWriter mockWriter;

    @InjectMocks
    private ProcessPaymentOutputService service;

    private Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> outputFileMap;

    @BeforeEach
    void setUp() {
        outputFileMap = new HashMap<>();
        outputFileMap.put(mockInputFile, mockOutputFile);
    }

    @Test
    void initialiseFiles_ShouldSetOutputFileMap() {
        service.initialiseFiles(outputFileMap);

        // Process a payment to verify the map was set correctly
        when(mockPaymentOutput.getPaymentRecord()).thenReturn(mockPaymentRecord);
        when(mockPaymentRecord.getCsvPaymentsInputFile()).thenReturn(mockInputFile);

        service.process(mockPaymentOutput);

        verify(mockPaymentRecord).setCsvPaymentsOutputFile(mockOutputFile);
    }

    @Test
    void process_ShouldSetOutputFileAndCallSuper() {
        service.initialiseFiles(outputFileMap);

        when(mockPaymentOutput.getPaymentRecord()).thenReturn(mockPaymentRecord);
        when(mockPaymentRecord.getCsvPaymentsInputFile()).thenReturn(mockInputFile);

        service.process(mockPaymentOutput);

        verify(mockPaymentRecord).setCsvPaymentsOutputFile(mockOutputFile);
        // Note: We can't verify super.process() call directly as it's not mockable
    }

    @Test
    void print_ShouldCallRepositoryFindAll() {
        List<CsvPaymentsOutputFile> outputFiles = List.of(mockOutputFile);
        when(csvPaymentsOutputFileRepository.findAll()).thenReturn(outputFiles);

        service.print();

        verify(csvPaymentsOutputFileRepository).findAll();
    }

    @Test
    void closeFiles_ShouldCloseAllWriters() throws IOException {
        when(mockOutputFile.getWriter()).thenReturn(mockWriter);
        List<CsvPaymentsOutputFile> outputFiles = List.of(mockOutputFile);

        service.closeFiles(outputFiles);

        verify(mockWriter).close();
    }

    @Test
    void closeFiles_ShouldContinueOnIOException() throws IOException {
        when(mockOutputFile.getWriter()).thenReturn(mockWriter);
        doThrow(new IOException()).when(mockWriter).close();
        List<CsvPaymentsOutputFile> outputFiles = List.of(mockOutputFile);

        // Should not throw exception
        assertDoesNotThrow(() -> service.closeFiles(outputFiles));
    }

    @Test
    void createCsvFile_ShouldCreateNewOutputFile() throws IOException {
        CsvPaymentsOutputFile result = service.createCsvFile(mockInputFile);

        assertNotNull(result);
        assertEquals(CsvPaymentsOutputFile.class, result.getClass());
    }
}
