package com.example.poc.client;

import com.example.poc.service.OrchestratorService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@QuarkusTest
class CsvPaymentsApplicationTest {

    @InjectMock
    OrchestratorService orchestratorService;

    @Inject
    CommandLine.IFactory factory;

    private ArgumentCaptor<String> csvFolderCaptor;
    private CsvPaymentsApplication application;

    @BeforeEach
    void setUp() {
        application = new CsvPaymentsApplication();
        application.orchestratorService = orchestratorService;
        application.factory = factory;

        csvFolderCaptor = ArgumentCaptor.forClass(String.class);
    }

    @SneakyThrows
    @Test
    void testRunWithDefaultCsvFolder() {
        // Given
        String[] args = new String[0]; // No args

        // When
        int exitCode = application.run(args);

        // Then
        assertEquals(0, exitCode);
        verify(orchestratorService).process(csvFolderCaptor.capture());
        assertEquals("csv/", csvFolderCaptor.getValue()); // Default value
    }

    @SneakyThrows
    @Test
    void testRunWithCustomCsvFolder() {
        // Given
        String customFolder = "/path/to/custom/csv";
        String[] args = new String[]{"--csv-folder=" + customFolder};

        // When
        int exitCode = application.run(args);

        // Then
        assertEquals(0, exitCode);
        verify(orchestratorService).process(csvFolderCaptor.capture());
        assertEquals(customFolder, csvFolderCaptor.getValue());
    }

    @SneakyThrows
    @Test
    void testRunWithShortFormOption() {
        // Given
        String customFolder = "/path/to/csv";
        String[] args = new String[]{"-c", customFolder};

        // When
        int exitCode = application.run(args);

        // Then
        assertEquals(0, exitCode);
        verify(orchestratorService).process(eq(customFolder));
    }

    @Test
    void testRunWithException() throws Exception {
        // Given
        String customFolder = "/path/with/error";

        // Create a spy of the application
        CsvPaymentsApplication appSpy = spy(application);
        appSpy.csvFolder = customFolder;

        // Setup the exception
        doThrow(new URISyntaxException("test", "Test URI syntax error"))
                .when(orchestratorService).process(eq(customFolder));

        // When - directly call run() to test exception handling
        appSpy.run();

        // Then
        verify(orchestratorService).process(eq(customFolder));
        // Verification passes if no exception is thrown from run(),
        // which means the application caught the exception as expected
    }

    @Test
    void testHelp() {
        // Given
        String[] args = new String[]{"--help"};

        // When
        int exitCode = application.run(args);

        // Then
        assertEquals(0, exitCode);
        // Help output goes to console, we're just verifying it doesn't crash
    }

    @Test
    void testVersion() {
        // Given
        String[] args = new String[]{"--version"};

        // When
        int exitCode = application.run(args);

        // Then
        assertEquals(0, exitCode);
        // Version output goes to console, we're just verifying it doesn't crash
    }
}