package ru.aglar;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    private Status status;
    private int length;
    private final ViewCallback view;

    private int stringOfFileNameLength;

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
                    case CloudProtocol.FILES_STRUCTURE_RESPONSE:
                        status = Status.STRING_OF_FILENAMES_LENGTH;
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
            if (status == Status.STRING_OF_FILENAMES_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    stringOfFileNameLength = buf.readInt();
                    status = Status.STRING_OF_FILENAMES;
                }
            }
            if (status == Status.STRING_OF_FILENAMES) {
                if (buf.readableBytes() >= stringOfFileNameLength) {
                    byte[] bytes = new byte[stringOfFileNameLength];
                    buf.readBytes(bytes);
                    String filenames = new String(bytes, StandardCharsets.UTF_8);
                    view.filledRemoteFiles(Arrays.asList(filenames.split(CloudProtocol.DELIMITER)));
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
        FREE, MSG_LENGTH, MESSAGE, STRING_OF_FILENAMES_LENGTH, STRING_OF_FILENAMES
    }
}