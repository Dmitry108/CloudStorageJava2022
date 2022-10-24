package ru.aglar;

import java.io.File;

public class FileInfo {
    private final String filename;
    private final long size;

    public FileInfo(File file) {
        this.filename = file.getName();
        this.size = file.isDirectory() ? -1L : file.length();
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }
}