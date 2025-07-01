package com.example.poc.service;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@QuarkusMain
@CommandLine.Command(
        name = "csv-payments",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Process CSV payment files"
)
public class CsvPaymentsApplication implements Runnable, QuarkusApplication {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

    @Inject
    OrchestratorService orchestratorService;

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Option(
            names = {"-c", "--csv-folder"},
            description = "The folder path containing CSV payment files (defaults to csv/ internal path)",
            defaultValue = "${env:CSV_FOLDER_PATH:-csv/}"
    )
    String csvFolder;

    @Override
    public int run(String... args) {
        return new CommandLine(this, factory).execute(args);
    }

    @Override
    public void run() {
        LOG.info("APPLICATION BEGINS processing {}", csvFolder);

        StopWatch watch = new StopWatch();
        watch.start();

        CountDownLatch latch = new CountDownLatch(1);

        try {
            orchestratorService.process(csvFolder)
                .subscribe().with(
                    result -> {
                        LOG.info(MessageFormat.format("Processing completed: {0}", result));
                        System.exit(0);
                    },
                    failure -> {
                        LOG.info(MessageFormat.format("Error: {0}", failure.getMessage()));
                        System.exit(1);
                    }
                );

            latch.await(); // block main thread here until completion

            watch.stop();
            LOG.info("✅ APPLICATION FINISHED processing of {} in {} seconds", csvFolder, watch.getTime(TimeUnit.SECONDS));

        } catch (URISyntaxException e) {
            watch.stop();
            LOG.error("❌ APPLICATION ABORTED due to invalid folder {} after {} seconds", csvFolder, watch.getTime(TimeUnit.SECONDS), e);
        } catch (Exception e) {
            watch.stop();
            LOG.error("❌ APPLICATION FAILED processing {} after {} seconds", csvFolder, watch.getTime(TimeUnit.SECONDS), e);
        }
    }

    // Traditional main method for standard Java execution
    public static void main(String[] args) {
        Quarkus.run(CsvPaymentsApplication.class, args);
    }
}
