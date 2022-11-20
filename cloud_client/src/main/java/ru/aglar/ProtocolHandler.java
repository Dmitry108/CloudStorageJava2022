package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status;
    private final ViewCallback view;
    private final static Path CLIENT_PATH = Paths.get("local_storage");
    private final BytesAnalyzer analyzer = new BytesAnalyzer();

    private Function<ByteBuf, Boolean> function;


    public ProtocolHandler(ViewCallback view) {
        this.status = Status.FREE;
        this.view = view;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (buf.readableBytes() > 0) {
            if (status == Status.FREE) {
                byte cmd = buf.readByte();
                status = Status.BUSY;
                switch (cmd) {
                    case CloudProtocol.FILES_STRUCTURE_RESPONSE:
                        analyzer.startOperation(cmd, new BytesAcceptListener() {
                            @Override
                            public void onSuccess(Object response) {
                                List<FileInfo> list = (List<FileInfo>) response;
                                view.fillRemoteFiles(list);
                            }
                        });
                        function = analyzer::getFileStructure;
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private enum Status {
        FREE, BUSY
    }
}