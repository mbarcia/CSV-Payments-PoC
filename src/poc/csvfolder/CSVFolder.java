package poc.csvfolder;

import poc.Processable;

public class CSVFolder extends Processable<CSVFolder> {
    private final String folderPath;

    public CSVFolder(String folderPath) {
        this.folderPath = folderPath;
    }

    public String toString() {
        return this.folderPath;
    }
}
