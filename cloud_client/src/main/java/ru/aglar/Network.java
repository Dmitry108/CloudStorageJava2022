package ru.aglar;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class Network {
    private SocketChannel socketChannel;

    private static Network instance;

    private Network() {
        instance = this;
    }

    public static Network getInstance() {
        if (instance == null) {
            instance = new Network();
        }
        return instance;
    }

    public void start(ResponseListener listener, Path storagePath, CountDownLatch cdl) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 888))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new ClientProtocolHandler(storagePath, listener));
                            instance.socketChannel = channel;
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.connect().sync();
            cdl.countDown();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void sendFile(Path file, ChannelFutureListener listener) {
        try {
            FileRegion region = new DefaultFileRegion(file.toFile(), 0, Files.size(file));
            ByteBuf header = CloudProtocol.getHeaderForSendingFile(file);
            socketChannel.write(header);
            ChannelFuture future = socketChannel.writeAndFlush(region);
            if (listener != null) {
                future.addListeners(listener);
            }
        } catch (IOException e) {
            e.printStackTrace();
            //отобразить ошибку
        }
    }

    public void sendFileRequest(String filename) {
        socketChannel.writeAndFlush(CloudProtocol.getFilesRequest(filename));
    }

    public void stop() {
        socketChannel.writeAndFlush(CloudProtocol.exit());
        socketChannel.close();
    }


}
