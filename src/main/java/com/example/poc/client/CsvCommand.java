package com.example.poc.client;

import com.example.poc.service.OrchestratorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "csv-payments",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Process CSV payment files"
)
@ApplicationScoped // Optional but allows deeper injection if needed
public class CsvCommand implements Runnable {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

    @Inject
    OrchestratorService orchestratorService;

    @CommandLine.Option(
            names = {"-c", "--csv-folder"},
            description = "The folder containing CSV payment files",
            defaultValue = "csv/"
    )
    String csvFolder;

    @Override
    public void run() {
        LOG.info("APPLICATION BEGINS");

        StopWatch watch = new StopWatch();
        watch.start();

        orchestratorService.process(csvFolder);

        watch.stop();
        LOG.info("APPLICATION FINISHED in {} seconds", watch.getTime(TimeUnit.SECONDS));
    }
}
