package ru.aglar;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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

    @Override
    public void onSuccess(Object response, Class<?> responseType) {

    }

    @Override
    public void onReceiveFile(FileInfo fileInfo) {
        channel.pipeline().writeAndFlush(CloudProtocol.transferMessageToByteBuf(
                String.format("File %s received on server!", fileInfo.getFilename())));
    }

    @Override
    public void onMessageReceive(String message) {
        System.out.println(message);
    }

    @Override
    public void onFileStructureReceive(List<FileInfo> filesList) {

    }
}