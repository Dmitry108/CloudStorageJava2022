package ru.aglar;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetIo implements Closeable {
    private boolean isRunning;
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    public NetIo(Socket socket) {
        try {
            this.isRunning = true;
            this.socket = socket;
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
            Thread thread = new Thread(this::readBytes);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readBytes() {

    }

    @Override
    public void close() throws IOException {
        os.writeUTF("quit");
        is.close();
        os.close();
        socket.close();
    }
}
