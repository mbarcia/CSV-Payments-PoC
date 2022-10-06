package poc.csvfile;

import poc.Processable;

import java.io.File;
import java.nio.file.Path;

public class CSVPaymentsFile extends Processable<CSVPaymentsFile> {
    private final File file;

    public CSVPaymentsFile(File file) {
        this.file = file;
    }

    public Path getPath() {
        return file.toPath();
    }
}
