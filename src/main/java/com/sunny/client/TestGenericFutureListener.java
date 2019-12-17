package com.sunny.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;

public class TestGenericFutureListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()){
            System.out.println("连接成功");
            Channel channel = channelFuture.channel();
            channel.writeAndFlush(Unpooled.copiedBuffer("dddd", //2
                    CharsetUtil.UTF_8));
        }else {
            throw new IOException("连接失败");
        }
    }

}
