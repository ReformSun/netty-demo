package com.sunny.flink.server;

import com.sunny.flink.io.NettyBufferPool;
import com.sunny.flink.io.NettyConfig;
import com.sunny.flink.io.NettyProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestNettyServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        NettyConfig nettyConfig = new NettyConfig(InetAddress.getLocalHost(),8000,1,1,"nio");
        NettyProtocol nettyProtocol = new NettyProtocol();
        NettyServer nettyServer = new NettyServer(nettyConfig);
        NettyBufferPool nettyBufferPool = new NettyBufferPool(10);
        nettyServer.init(nettyProtocol,nettyBufferPool);
    }
}
