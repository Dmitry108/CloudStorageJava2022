package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.charset.StandardCharsets;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status;
    private int length;
    private final ViewCallback view;

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
                switch (cmd) {
                    case CloudProtocol.MESSAGE:
                        status = Status.MSG_LENGTH;
                        break;
                    default:
                        ctx.writeAndFlush(CloudProtocol.transferMessageToByteBuf("On client unknown format!"));
                }
            }
            if (status == Status.MSG_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    length = buf.readInt();
                    status = Status.MESSAGE;
                }
            }
            if (status == Status.MESSAGE) {
                if (buf.readableBytes() >= length) {
                    byte[] messageByteArray = new byte[length];
                    buf.readBytes(messageByteArray);
                    view.onReceiveMessage(new String(messageByteArray, StandardCharsets.UTF_8));
                    status = Status.FREE;
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
        FREE, MSG_LENGTH, MESSAGE
    }
}