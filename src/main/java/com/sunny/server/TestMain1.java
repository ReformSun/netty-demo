package com.sunny.server;

import com.sunny.flink.io.NettyBufferPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class TestMain1 {
    public static void main(String[] args) {
//        testMethod3();
        testMethod4();
    }

    public static void testMethod1(){
        int port = 8888;        //1
        try {
            new EchoServer(port).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMethod2(){
        int port = 8888;        //1
        NettyBufferPool nettyBufferPool = new NettyBufferPool(514);
        new Thread(()->{

        });
        try {
            new EchoServer(port).start1(nettyBufferPool);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用telnet localhost 8000 进行测试
     */
    public static void testMethod3(){
        NettyBufferPool nettyBufferPool = new NettyBufferPool(514);
        ChannelInitializer channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch)
                    throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new StringProcessHandler());
            }
        };
        try {
            EchoServer.common(nettyBufferPool,channelInitializer,8000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用telnet localhost 8000 进行测试
     */
    public static void testMethod4(){
        LengthFieldBasedFrameDecoder spliter=new LengthFieldBasedFrameDecoder(1024,0,4,0,4);
        ChannelInitializer channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            @Override
            public void initChannel(EmbeddedChannel ch)
                    throws Exception {
                ch.pipeline().addLast(spliter);
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new StringProcessHandler());
            }
        };
        try {
            EmbeddedChannel embeddedChannel = new EmbeddedChannel(channelInitializer);
            for (int i = 0; i < 100; i++) {
                ByteBuf byteBuf = Unpooled.buffer();
                String message = "咯咯，I am " + i;
                byte[] bytes = message.getBytes("UTF-8");
                byteBuf.writeInt(bytes.length);
                byteBuf.writeBytes(bytes);
                embeddedChannel.writeInbound(byteBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
