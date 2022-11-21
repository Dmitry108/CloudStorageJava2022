package ru.aglar;

import java.util.List;

public interface ViewCallback {
    void onReceiveMessage(String message);
    void filledRemoteFiles(List<String> filenames);
}