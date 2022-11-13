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
    private Callback callback;
    private byte[] buf;

    public NetIo(Socket socket, Callback callback) {
        try {
            this.isRunning = true;
            this.socket = socket;
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
            this.callback = callback;
            this.buf = new byte[4096];
            Thread thread = new Thread(this::readMessage);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) throws IOException {
        os.writeUTF(message);
        os.flush();
    }

    public void sendFileSize(long size) throws IOException {
        os.writeLong(size);
        os.flush();
    }

    public void sendFile(byte[] buf, int off, int len) throws IOException {
        os.write(buf, off, len);
        os.flush();
    }

    private void readMessage() {
        while (true) {
            try {
                String message = is.readUTF();
                callback.onReceive(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void close() throws IOException {
        os.writeUTF("quit");
        is.close();
        os.close();
        socket.close();
    }
}
