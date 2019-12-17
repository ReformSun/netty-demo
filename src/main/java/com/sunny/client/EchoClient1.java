package com.sunny.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class EchoClient1 {
    private final String host;
    private final int port;

    public EchoClient1(String host, int port) {
        this.host = host;
        this.port = port;
    }
    /**
     * 通过测试发现 通过EchoClientHandler的channelActive发送信息和TestGenericFutureListener监听方法发送。会
     * 首先进入监听方法然后进入处理这方法，两个地方发送的数据会连接到一起
     */

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();                //1
            b.group(group)                                //2
                    .channel(NioSocketChannel.class)            //3
                    .remoteAddress(new InetSocketAddress(host, port))    //4
                    .handler(new ChannelInitializer<SocketChannel>() {    //5
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    );
                        }
                    });

            ChannelFuture f = b.connect().addListener(new TestGenericFutureListener()).sync();        //6

            f.channel().closeFuture().sync();            //7
        } finally {
            group.shutdownGracefully().sync();            //8
        }
    }
}
