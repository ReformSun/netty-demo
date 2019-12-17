package com.sunny.flink.client;

import com.sunny.flink.io.NettyBufferPool;
import com.sunny.flink.io.NettyConfig;
import com.sunny.flink.io.NettyMessage;
import com.sunny.flink.io.NettyProtocol;
import com.sunny.flink.server.NettyServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class TestNettyClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        NettyConfig nettyConfig = new NettyConfig(InetAddress.getLocalHost(),8001,1,1,"nio");
        NettyProtocol nettyProtocol = new NettyProtocol();
        NettyClient nettyClient = new NettyClient(nettyConfig);
        NettyBufferPool nettyBufferPool = new NettyBufferPool(1000);
        nettyClient.init(nettyProtocol,nettyBufferPool);

        ChannelFuture channelFuture = nettyClient.connect(new InetSocketAddress(8000)).sync();
        final ChannelFutureListener listener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    System.out.println("成功");
                }
            }
        };
        NettyMessage.TestRequest testRequest = new NettyMessage.TestRequest(1000,10002);
        ChannelFuture channelFuture1 = channelFuture.channel().writeAndFlush(testRequest);
        channelFuture1.addListener(listener);

        channelFuture.channel().closeFuture().sync();
    }
}
