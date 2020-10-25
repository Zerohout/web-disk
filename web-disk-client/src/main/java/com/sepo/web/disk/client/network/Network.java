package com.sepo.web.disk.client.network;

import com.sepo.web.disk.client.controllers.SignInController;
import com.sepo.web.disk.client.handlers.MainHandler;
import com.sepo.web.disk.client.handlers.AuthHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {
    private static final Logger logger = LogManager.getLogger(Network.class);
    public static AuthHandler authHandler;
    public static MainHandler mainHandler;
    private SocketChannel channel;


    private static Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    private Network() {
    }


    public void start(CountDownLatch countDownLatch, CountDownLatch handlerLatch) {
        var group = new NioEventLoopGroup();
        try {
            logger.info("creating bootstrap");
            var bootstrap = new Bootstrap();
            logger.info("creating bootstrap group");
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 8189))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            logger.info("init channel");
                            socketChannel.pipeline().addLast(
                                    new AuthHandler());
                            channel = socketChannel;
                            handlerLatch.countDown();
                            logger.info("added pipeline with AuthHandler");
                        }
                    });

            var channelFuture = bootstrap.connect().sync();
            countDownLatch.countDown();

            authHandler.onSuccessfulConnection();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            authHandler.onErrorConnectionAction();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }

    //
//    public void setNetworkHandler() {
//        this.networkHandler = new NetworkHandler();
//    }
//
//    public NetworkHandler getNetworkHandler(){
//        return this.networkHandler;
//    }


     public void stop() {
        channel.close();
    }
}
