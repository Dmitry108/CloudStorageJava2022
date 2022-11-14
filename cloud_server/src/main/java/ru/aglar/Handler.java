package ru.aglar;

import java.io.*;
import java.net.Socket;

public class Handler implements Runnable, Closeable {

    private static final int SIZE = 4096;

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private File serverDir;

    public Handler(Socket socket) {
        try {
            this.socket = socket;
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
            this.serverDir = new File("cloud_server", "cloud_storage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String command = is.readUTF();
                if (command.equals("$file")) {
                    String filename = is.readUTF();
                    long size = is.readLong();
                    byte[] buf = new byte[SIZE];
                    try (FileOutputStream fos = new FileOutputStream(new File(serverDir, filename))) {
                        for (int i = 0, read; i < (size + SIZE - 1) / SIZE; i++) {
                            read = is.read(buf);
                            fos.write(buf, 0, read);
                        }
                        os.writeUTF("Success! File received on server!");
                    } catch (IOException e) {
                        int n;
                        for (int i = 0, read; i < (size + SIZE - 1) / SIZE; i++) {
                            read = is.read(buf);

                        }
                        System.out.println("fff");
                        os.writeUTF("Error on receiving file!");
                        e.printStackTrace();
                    }
                } else if (command.equals("quit")) {
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