package ru.aglar;

import java.io.File;

public class FileInfo {
    private String filename;
    private long size;

    public FileInfo(File file) {
        this.filename = file.getName();
        this.size = file.isDirectory() ? -1L : file.length();
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