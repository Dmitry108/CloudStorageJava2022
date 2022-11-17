package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status = Status.FREE;

    private long receivedFileSize;
    private int filenameLength;
    private String filename;
    private long fileSize;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (buf.readableBytes() > 0) {
            if (status == Status.FREE) {
                byte cmd = buf.readByte();
                switch (cmd) {
                    case CloudProtocol.ACCEPT_FILE:
                        status = Status.FILENAME_LENGTH;
                        receivedFileSize = 0;
                        break;
                    case CloudProtocol.FILES_STRUCTURE_REQUEST:
                        status = Status.FILES_LIST;
                        break;
                    default:
                        ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf("Unknown format on server!"));
                }
            }
            if (status == Status.FILENAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    filenameLength = buf.readInt();
                    status = Status.FILENAME;
                }
            }
            if (status == Status.FILENAME) {
                if (buf.readableBytes() >= filenameLength) {
                    byte[] nameByteArray = new byte[filenameLength];
                    buf.readBytes(nameByteArray);
                    filename = new String(nameByteArray, StandardCharsets.UTF_8);
                    status = Status.FILE_SIZE;
                }
            }
            if (status == Status.FILE_SIZE) {
                if (buf.readableBytes() >= 8)
                fileSize = buf.readLong();
                status = Status.FILE;
            }
            if (status == Status.FILE) {
                //обработать ситуацию если файл уже существует
                try (BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(Paths.get("server_storage", filename).toFile()))) {
                    while (buf.readableBytes() > 0) {
                        out.write(buf.readByte());
                        receivedFileSize++;
                        if (fileSize == receivedFileSize) {
                            ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf(String.format("File %s received on server", filename)));
                            status = Status.FREE;
                            break;
                        }
                    }
                } catch (IOException e) {
                    //удалить остальные байты этого файла из буфера
                    ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf("Error on receiving file!"));
                    e.printStackTrace();
                }
            }
            if (status == Status.FILES_LIST) {
                Path path = Paths.get("server_storage");
                String filenames = Files.list(path)
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
//                                .collect(Collectors.toList());
                        .collect(Collectors.joining(CloudProtocol.DELIMITER));
                ctx.writeAndFlush(CloudProtocol.transferStringOfFilenamesToByteBuf(filenames));
                status = Status.FREE;
            }
        }
        buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private enum Status {
        FREE, FILENAME_LENGTH, FILENAME, FILE_SIZE, FILE, FILES_LIST
    }
}