package com.example.poc.biz;

import java.io.File;

public class CSVPaymentsFile {
    private final File file;

    public CSVPaymentsFile(File file) {
        this.file = file;
    }

    public String getPath() {
        return file.getPath();
    }
}
