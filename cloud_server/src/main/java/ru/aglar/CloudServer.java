package ru.aglar;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CloudServer implements ResponseListener {
    private final static Path STORAGE_PATH = Paths.get("server_storage");
    private SocketChannel channel;
    private final ResponseListener listener = this;

    static {
            try {
                if (Files.notExists(STORAGE_PATH)) {
                    Files.createDirectory(STORAGE_PATH);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            channel = socketChannel;
                            socketChannel.pipeline().addLast(new ServerProtocolHandler(STORAGE_PATH, listener));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture serverFuture = bootstrap.bind(888).sync();
            System.out.println("Server start...");
            serverFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new CloudServer().start();
    }

    private void sendMessage(String message) {
        channel.pipeline().writeAndFlush(CloudProtocol.transferMessageToByteBuf(message));

    }

    @Override
    public void onReceiveFile(FileInfo fileInfo) {
        sendMessage(String.format("File %s received on server!", fileInfo.getFilename()));
    }

    @Override
    public void onMessageReceive(String message) {
        System.out.println(message);
    }

    @Override
    public void onFileStructureReceive(List<FileInfo> filesList) {

    }

    @Override
    public void sendFile(String filename) {
        Path file = STORAGE_PATH.resolve(filename);
        if (!Files.exists(file)) {
            sendMessage(String.format("File %s doesn't exist!", filename));
            return;
        }
//        подумать над унификацией кода клиента и сервера
        try {
            FileRegion region = new DefaultFileRegion(file.toFile(), 0, Files.size(file));
            ByteBuf header = CloudProtocol.getHeaderForSendingFile(file);
            channel.write(header);
            ChannelFuture future = channel.writeAndFlush(region);
            future.addListeners(channelFuture -> System.out.println("File has been sent to client"));
        } catch (IOException e) {
            e.printStackTrace();
            //отобразить ошибку
        }
    }

    @Override
    public void onExit() {
        channel.close();
    }
}