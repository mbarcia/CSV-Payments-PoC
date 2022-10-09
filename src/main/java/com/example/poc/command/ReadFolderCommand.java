package com.example.poc.command;

import com.example.poc.biz.CSVFolder;
import com.example.poc.biz.CSVPaymentsFile;
import com.example.poc.repository.CSVPaymentsFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class ReadFolderCommand extends BaseCommand<CSVFolder, Set<CSVPaymentsFile>> {
    @Autowired
    private CSVPaymentsFileRepository csvPaymentsFileRepository;

    @Override
    public Set<CSVPaymentsFile> execute(CSVFolder csvFolder) {
        super.execute(csvFolder);

        Set<CSVPaymentsFile> fileList = new HashSet<>();
        try {
            for (File result : Objects.requireNonNull(getFileList(csvFolder.toString()))) {
                CSVPaymentsFile csvPaymentsFile = new CSVPaymentsFile(result);
                csvPaymentsFileRepository.save(csvPaymentsFile);
                fileList.add(csvPaymentsFile);
            }
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getLocalizedMessage());
        }

        return fileList;
    }

    /**
     * @param dir String path
     * @return A set of files resulting from traversing the directory to one or
     * more levels deeper than its direct file entries
     * @see <a href="https://www.baeldung.com/java-list-directory-files#walking">Reference</a>
     */
    private Set<File> getFileList(String dir) throws IOException {
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
