package ru.aglar;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerIo {

    public ServerIo() {
        try (ServerSocket server = new ServerSocket(888)) {
            System.out.println("Server started...");
            while (true) {
                try {
                    Socket socket = server.accept();
                    System.out.println("Client connected...");
                    new Thread(new Handler(socket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ServerIo();
    }
}
