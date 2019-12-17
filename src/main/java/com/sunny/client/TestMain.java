package com.sunny.client;

import com.sunny.flink.io.NettyBufferPool;

public class TestMain {
    public static void main(String[] args) {

    }

    public static void testMethod1(){
        final String host = "localhost";
        final int port = 8888;
        try {
            new EchoClient(host, port).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMethod2(){
        NettyBufferPool nettyBufferPool = new NettyBufferPool(514);
        final String host = "localhost";
        final int port = 8888;
        try {
            new EchoClient(host, port).start1(nettyBufferPool);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
