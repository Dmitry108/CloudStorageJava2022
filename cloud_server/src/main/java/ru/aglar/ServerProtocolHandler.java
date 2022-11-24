package ru.aglar;

import io.netty.channel.ChannelHandlerContext;
import java.nio.file.Path;

public class ServerProtocolHandler extends ProtocolHandler {

    public ServerProtocolHandler(Path storagePath, ResponseListener listener) {
        super(storagePath, listener);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(CloudProtocol.exit());
        System.out.println("Client disconnected...");
    }
}