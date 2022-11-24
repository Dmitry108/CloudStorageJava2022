package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status = Status.FREE;

    private final Path storagePath;
    private final BytesAnalyzer analyzer;
    private Function<ByteBuf, Boolean> function;

    public ProtocolHandler(Path storagePath, ResponseListener listener) {
        this.storagePath = storagePath;
        this.analyzer = new BytesAnalyzer(storagePath, listener);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (buf.readableBytes() > 0) {
            if (status == Status.FREE) {
                byte cmd = buf.readByte();
                status = Status.BUSY;
                switch (cmd) {
                    case CloudProtocol.ACCEPT_FILE:
                        analyzer.startOperation(cmd);
                        function = analyzer::acceptFile;
                        break;
                    case CloudProtocol.FILE_REQUEST:
                        analyzer.startOperation(cmd);
                        function = analyzer::fileRequest;
                        break;
                    case CloudProtocol.FILES_STRUCTURE_REQUEST:
                        sendFileStructure(ctx);
                        break;
                    case CloudProtocol.FILES_STRUCTURE_RESPONSE:
                        analyzer.startOperation(cmd);
                        function = analyzer::getFileStructure;
                        break;
                    case CloudProtocol.MESSAGE:
                        analyzer.startOperation(cmd);
                        function = analyzer::acceptMessage;
                        break;
                    default:
                        status = Status.FREE;
                        buf.clear();
                        ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf("Unknown format! " + cmd));
                }
            }
            if (status == Status.BUSY) {
                long count = analyzer.getExpectedCountBytes();
                if (count > buf.readableBytes()) count = buf.readableBytes();
                if (buf.readableBytes() >= count) {
                    if (function.apply(buf.readBytes((int) count))) {
                        status = Status.FREE;
                    }
                }
            }
        }
        buf.release();
    }

    private void sendFileStructure(ChannelHandlerContext ctx) throws IOException {
        List<FileInfo> files = Files.list(storagePath)
                .filter(p -> !Files.isDirectory(p))
                .map(p -> new FileInfo(p.getFileName().toString(), p.toFile().length()))
                .collect(Collectors.toList());
        ctx.write(CloudProtocol.getHeaderOfFileStructure(files.size()));
        files.forEach(fileInfo -> {
            ctx.write(CloudProtocol.getFileInfoByteBuf(fileInfo));
        });
        ctx.flush();
        status = Status.FREE;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private enum Status {
        FREE, BUSY
    }
}