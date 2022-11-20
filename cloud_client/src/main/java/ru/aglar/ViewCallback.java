package ru.aglar;

import java.util.List;

public interface ViewCallback {
    void onReceiveMessage(String message);
    void fillRemoteFiles(List<FileInfo> files);
}