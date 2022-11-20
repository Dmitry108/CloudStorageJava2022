package ru.aglar;

import io.netty.buffer.ByteBuf;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BytesAnalyzer {
    private int stepOfOperation = 0;
    private long expectedCountBytes = 0;
    private int counter;
    private Object object;
    private BytesAcceptListener listener;

    private void setControl(int stepOfOperation, long expectedCountBytes) {
        this.stepOfOperation = stepOfOperation;
        this.expectedCountBytes = expectedCountBytes;
    }

    public boolean acceptFile(ByteBuf buf) {
        if (stepOfOperation == 1) {
            setControl(2, buf.readInt() + 8);
        } else if (stepOfOperation == 2) {
            FileInfo fileInfo = (FileInfo) object;
            byte[] filenameBytes = new byte[(int) expectedCountBytes - 8];
            buf.readBytes(filenameBytes);
            long size = buf.readLong();
            fileInfo.setFilename(new String(filenameBytes, StandardCharsets.UTF_8));
            fileInfo.setSize(size);
            setControl(3, size);
            counter = 0;
        } else if (stepOfOperation == 3) {
            //обработать ситуацию если файл уже существует
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(Paths.get("server_storage",
                    ((FileInfo) object).getFilename()).toFile(), true))) {
                FileInfo fileInfo = (FileInfo) object;
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    counter++;
                    if (fileInfo.getSize() == counter) {
                        listener.onSuccess(String.format("File %s received on server!", fileInfo.getFilename()));
                        setControl(0, 0);
                        return true;
                    }
                }
                expectedCountBytes = fileInfo.getSize() - counter;
            } catch (IOException e) {
                //удалить остальные байты этого файла из буфера
                throw new RuntimeException("Error on receiving file!");
            }
        }
        return false;
    }

    public long getExpectedCountBytes() {
        return expectedCountBytes;
    }

    public void startOperation(byte cmd, BytesAcceptListener listener) {
        this.listener = listener;
        stepOfOperation = 1;
        if (cmd == CloudProtocol.ACCEPT_FILE) {
            expectedCountBytes = 4;
            object = new FileInfo();
        } else if (cmd == CloudProtocol.MESSAGE) {
            expectedCountBytes = 4;
            object = new StringBuffer();
        } else if (cmd == CloudProtocol.FILES_STRUCTURE_RESPONSE) {
            expectedCountBytes = 4;
            object = new ArrayList<FileInfo>();
        }
    }

    public boolean acceptMessage(ByteBuf buf) {
        if (stepOfOperation == 1) {
            setControl(2, buf.readInt());
        } else if (stepOfOperation == 2) {
            byte[] messageBytes = new byte[(int) expectedCountBytes];
            buf.readBytes(messageBytes);
            ((StringBuffer) object).append(new String(messageBytes, StandardCharsets.UTF_8));
            setControl(0, 0);
            listener.onSuccess(((StringBuffer) object).toString());
            return true;
        }
        return false;
    }

    public Boolean getFileStructure(ByteBuf buf) {
        if (stepOfOperation == 1) {
            counter = buf.readInt();
            if (counter == 0) {
                setControl(0, 0);
                listener.onSuccess(object);
                return true;
            }
            setControl(2, 4);
        } else if (stepOfOperation == 2) {
            setControl(3, buf.readInt() + 8);
        } else if (stepOfOperation == 3) {
            byte[] filenameBytes = new byte[(int) expectedCountBytes - 8];
            buf.readBytes(filenameBytes);
            long size = buf.readLong();
            List<FileInfo> list = (List<FileInfo>) object;
            list.add(new FileInfo(new String(filenameBytes, StandardCharsets.UTF_8), size));
            if (counter == list.size()) {
                setControl(0, 0);
                listener.onSuccess(object);
                return true;
            }
            setControl(2, 4);
        }
        return false;
    }
}