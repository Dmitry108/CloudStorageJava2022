package ru.aglar;

import java.util.List;

public interface ResponseListener {
    void onSuccess(Object response, Class<?> responseType);
    void onReceiveFile(FileInfo fileInfo);
    void onMessageReceive(String message);
    void onFileStructureReceive(List<FileInfo> filesList);
}