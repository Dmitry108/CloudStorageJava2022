package ru.aglar;

import java.nio.file.Path;

public class ClientProtocolHandler extends ProtocolHandler {

    public ClientProtocolHandler(Path storagePath, ResponseListener listener) {
        super(storagePath, listener);
    }
}