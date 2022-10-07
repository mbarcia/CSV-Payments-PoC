package com.example.poc.command;

import com.example.poc.Command;
import com.example.poc.biz.CSVPaymentsFile;
import com.example.poc.biz.CSVFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadFolderCommand implements Command<CSVFolder, Set<CSVPaymentsFile>> {
    @Override
    public Set<CSVPaymentsFile> execute(CSVFolder csvFolder) {
        Set<CSVPaymentsFile> fileList = new HashSet<>();
        for (File result: Objects.requireNonNull(getFileList(csvFolder.toString()))) {
            fileList.add(new CSVPaymentsFile(result));
        }

        return fileList;
    }

    /**
     * @param dir String path
     * @return A set of files resulting from traversing the directory to one or
     * more levels deeper than its direct file entries
     *
     * @see <a href="https://www.baeldung.com/java-list-directory-files#walking">Reference</a>
     */
    private Set<File> getFileList(String dir) {
        Set<File> fileList = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!Files.isDirectory(file)) {
                        fileList.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Logger logger = Logger.getLogger(String.valueOf(CSVFolder.class));
            logger.log(Level.SEVERE, e.getLocalizedMessage());
            return null;
        }

        return fileList;
    }
}
