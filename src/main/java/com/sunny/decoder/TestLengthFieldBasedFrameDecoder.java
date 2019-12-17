package com.sunny.decoder;

import com.sunny.flink.io.NettyMessage;
import com.sunny.server.StringProcessHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * （1） maxFrameLength - 发送的数据包最大长度；
 *（2） lengthFieldOffset - 长度域偏移量，指的是长度域位于整个数据包字节数组中的下标；
 *（3） lengthFieldLength - 长度域的自己的字节数长度。
 *（4） lengthAdjustment – 长度域的偏移量矫正。 如果长度域的值，除了包含有效数据域的长度外，还包含了其他域（如长度域自身）长度，那么，就需要进行矫正。矫正的值为：包长 - 长度域的值 – 长度域偏移 – 长度域长。
 *（5） initialBytesToStrip – 丢弃的起始字节数。丢弃处于有效数据前面的字节数量。比如前面有4个节点的长度域，则它的值为4。
 */
public class TestLengthFieldBasedFrameDecoder {
    public static void main(String[] args) {
        testMethod2();
    }

    public static void testMethod1(){
        try {
            EmbeddedChannel embeddedChannel = getEmbeddedChannel(0,4,0,4);
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

    /**
     * 长度域 存储消息体长度的域
     * 长度域偏移量 指定存储长度域的起始下标位置
     * 长度域的长度
     * 如果长度域的值除了包含有消息体的长度 或者还自己长度域的长度、消息头的长度，那么就要进行校正
     * 丢失起始字节数
     */
    public static void testMethod2(){
        try {
            EmbeddedChannel embeddedChannel = getEmbeddedChannel(0,4,0,4);
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



    private static EmbeddedChannel getEmbeddedChannel(
                                          int lengthFieldOffset, int lengthFieldLength,
                                          int lengthAdjustment, int initialBytesToStrip){
        LengthFieldBasedFrameDecoder spliter=new LengthFieldBasedFrameDecoder(1024,lengthFieldOffset,lengthFieldLength,lengthAdjustment,initialBytesToStrip);
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
            return embeddedChannel;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("",e);
        }

    }
}
