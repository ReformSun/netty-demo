package com.sunny.server;

import com.sunny.flink.io.NettyBufferPool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }
    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(); //3
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)                                //4
                    .channel(NioServerSocketChannel.class)        //5
                    .localAddress(new InetSocketAddress(port))    //6
                    .childHandler(new ChannelInitializer<SocketChannel>() { //7
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new EchoServerHandler());
                        }
                    });

            ChannelFuture f = b.bind().sync();            //8
            System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
            f.channel().closeFuture().sync();            //9
        } finally {
            group.shutdownGracefully().sync();            //10
        }
    }

    public void start1(NettyBufferPool nettyBufferPool) throws Exception {
        common(nettyBufferPool,new ChannelInitializer<SocketChannel>() { //7
            @Override
            public void initChannel(SocketChannel ch)
                    throws Exception {
                ch.pipeline().addLast(
                        new EchoServerHandler());
            }
        },port);
    }



    public static void common(NettyBufferPool nettyBufferPool,ChannelInitializer channelInitializer,int port) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(); //3
        try {
            ServerBootstrap b = new ServerBootstrap();
            // 绑定地址
            b.localAddress(new InetSocketAddress(port));
            b.option(ChannelOption.ALLOCATOR,nettyBufferPool);
            b.childOption(ChannelOption.ALLOCATOR,nettyBufferPool);
            // 接收和发送的尺寸
            b.childOption(ChannelOption.SO_RCVBUF,256);
            b.childOption(ChannelOption.SO_SNDBUF,256);


            b.group(group)                                //4
                    .channel(NioServerSocketChannel.class)        //5
                    .childHandler(channelInitializer);

            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();            //10
        }
    }

    public static Channel getChannel(NettyBufferPool nettyBufferPool, ChannelInitializer channelInitializer, int port) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(); //3
        try {
            ServerBootstrap b = new ServerBootstrap();
            // 绑定地址
            b.localAddress(new InetSocketAddress(port));
            b.option(ChannelOption.ALLOCATOR,nettyBufferPool);
            b.childOption(ChannelOption.ALLOCATOR,nettyBufferPool);
            // 接收和发送的尺寸
            b.childOption(ChannelOption.SO_RCVBUF,256);
            b.childOption(ChannelOption.SO_SNDBUF,256);


            b.group(group)                                //4
                    .channel(NioServerSocketChannel.class)        //5
                    .childHandler(channelInitializer);

            ChannelFuture f = b.bind().sync();
            return f.channel();
        } finally {
            group.shutdownGracefully().sync();            //10
        }
    }
}
