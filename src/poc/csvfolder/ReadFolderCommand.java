package poc.csvfolder;

import poc.Command;
import poc.csvfile.CSVPaymentsFile;
import poc.csvfile.ReadFileCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadFolderCommand implements Command<CSVFolder> {
    @Override
    public void execute(CSVFolder csvFolder) {
        Set<File> fileList;
        try {
            fileList = getFileList(csvFolder);
        } catch (IOException e) {
            Logger logger = Logger.getLogger(String.valueOf(CSVFolder.class));
            logger.log(Level.SEVERE, e.getLocalizedMessage());
            return;
        }

        fileList.forEach(f -> {
            CSVPaymentsFile csvFile = new CSVPaymentsFile(f);

            // Commands creation
            Command<CSVPaymentsFile> readPayments = new ReadFileCommand();
            // Commands list assignment
            csvFile.setCommandList(List.of(readPayments));
            // Make it all happen
            csvFile.forEach(command -> command.execute(csvFile));
        });
    }

    private Set<File> getFileList(CSVFolder csvFolder) throws IOException {
        String dir = csvFolder.toString();

        // adapted from https://www.baeldung.com/java-list-directory-files#walking
        Set<File> fileList = new HashSet<>();
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!Files.isDirectory(file)) {
                    fileList.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return fileList;
    }
}
