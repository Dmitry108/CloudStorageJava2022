package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CloudProtocol {
    public final static byte ACCEPT_FILE = 11;
    //протокол передачи файла: 1 байт команда -> 4 байта размер имени файла -> имя файла -> 8 байт размер файла -> файл
    public final static byte FILES_STRUCTURE_REQUEST = 12;
    //протокол запроса структуры файлов: 1 байт команда
    public final static byte FILES_STRUCTURE_RESPONSE = 13;
    //протокол передачи структуры данных: 1 байт команда -> 4 байта размер строки -> строка имен файлов, разделенных CloudProtocol.DELIMITER
    public final static byte MESSAGE = 21;
    //протокол: 1 байт команда -> 4 байта размер сообщения -> сообщение
    public final static String DELIMITER = "@@";

    public static ByteBuf transferMessageToByteBuf(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1 + 4 + length);
        buf.writeByte(MESSAGE).writeInt(length).writeBytes(bytes);
        return buf;
    }

    public static ByteBuf getFilesStructureRequest() {
        return ByteBufAllocator.DEFAULT.buffer(1).writeByte(FILES_STRUCTURE_REQUEST);
    }

    public static ByteBuf getHeaderForSendingFile(Path file) throws IOException {
        byte[] filename = file.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        int filenameLength = filename.length;
        long fileSize = Files.size(file);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameLength + 8);
        buf.writeByte(ACCEPT_FILE).writeInt(filenameLength).writeBytes(filename).writeLong(fileSize);
        return buf;
    }

    public static ByteBuf transferStringOfFilenamesToByteBuf(String filenames) {
        byte[] bytes = filenames.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1 + bytes.length);
        buf.writeByte(FILES_STRUCTURE_RESPONSE).writeInt(filenames.length()).writeBytes(bytes);
        return buf;
    }
}