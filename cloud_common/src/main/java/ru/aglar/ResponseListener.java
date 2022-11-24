package ru.aglar;

import java.util.List;

public interface ResponseListener {
    void onReceiveFile(FileInfo fileInfo);
    void onMessageReceive(String message);
    void onFileStructureReceive(List<FileInfo> filesList);
    void sendFile(String filename);
    void onExit();
}