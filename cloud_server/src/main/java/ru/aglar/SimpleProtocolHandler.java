package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status = Status.FREE;

    private final static Path SERVER_PATH = Paths.get("server_storage");
    private final BytesAnalyzer analyzer = new BytesAnalyzer();

    private Function<ByteBuf, Boolean> function;

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
                status = Status.BUSY;
                switch (cmd) {
                    case CloudProtocol.ACCEPT_FILE:
                        analyzer.startOperation(cmd, message -> {
                                ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf((String) message));
                                status = Status.FREE;
                        });
                        function = analyzer::acceptFile;
                        break;
                    case CloudProtocol.FILES_STRUCTURE_REQUEST:
                        sendFileStructure(ctx);
                        break;
                    case CloudProtocol.MESSAGE:
                        analyzer.startOperation(cmd, System.out::println);
                        function = analyzer::acceptMessage;
                        break;
                    default:
                        status = Status.FREE;
                        buf.clear();
                        ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf("Unknown format on server!" + cmd));
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
        List<FileInfo> files = Files.list(SERVER_PATH)
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