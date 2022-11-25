package ru.aglar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
    private String filename;
    private long size;

    public FileInfo(Path file) {
        try {
            this.filename = file.getFileName().toString();
            this.size = Files.isDirectory(file) ? -1L : Files.size(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileInfo(String filename, long size) {
        this.filename = filename;
        this.size = size;
    }

    public FileInfo() {}

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setSize(long size) {
        this.size = size;
    }
}