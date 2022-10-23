package ru.aglar;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Handler implements Runnable, Closeable {

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    public Handler(Socket socket) {
        try {
            this.socket = socket;
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String command = is.readUTF();
                if (command.equals("quit")) {
                    close();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        System.out.println("Client disconnected...");
        is.close();
        os.close();
        socket.close();
    }
}