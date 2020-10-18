package com.sepo.web.disk.client.network;

import com.sepo.web.disk.client.Helpers.OnActionCallback;
import com.sepo.web.disk.client.handlers.NetworkHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {
    private static Network ourInstance = new Network();
    private Channel currentChannel;
    private NetworkHandler networkHandler;




    public static Network getInstance() {
        return ourInstance;
    }
    private Network() {
    }


    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
        var group = new NioEventLoopGroup();
        try {
            var bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 8189))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    networkHandler);
                            currentChannel = socketChannel;
                        }
                    });

            var channelFuture = bootstrap.connect().sync();
            countDownLatch.countDown();

            networkHandler.onSuccessfulConnection();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            networkHandler.onErrorConnectionAction();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    public void setNetworkHandler() {
        this.networkHandler = new NetworkHandler();
    }

    public NetworkHandler getNetworkHandler(){
        return this.networkHandler;
    }



    public void stop() {
        currentChannel.close();
    }
}
