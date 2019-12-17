package com.sunny.flink.io;

import com.sunny.flink.io.NettyMessage;
import com.sunny.server.StringProcessHandler;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;

import java.net.ProtocolException;
import java.net.SocketAddress;

import static com.sunny.flink.io.NettyMessage.MAGIC_NUMBER;

public class TestFlinkBuf {
    private static final NettyMessage.NettyMessageEncoder
            messageEncoder = new NettyMessage.NettyMessageEncoder();
    private static final NettyMessageDecoder
            messageDecoder = new NettyMessageDecoder(true);
    public static void main(String[] args) {
//        testMethod2();
//        testMethod2_1();
        testMethod3();
    }

    public static void testMethod1() throws Exception {
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        NettyMessage.TestRequest testRequest = new NettyMessage.TestRequest(100,100);

        ByteBuf byteBuf = testRequest.write(byteBufAllocator);

    }


    public static void testMethod2(){
        ChannelInitializer channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            @Override
            public void initChannel(EmbeddedChannel ch)
                    throws Exception {
                ch.pipeline().addLast(messageDecoder);
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new StringProcessHandler());
            }
        };
        try {
            EmbeddedChannel embeddedChannel = new EmbeddedChannel(channelInitializer);
            for (int i = 0; i < 100; i++) {
                String message = "ddd" + i;
                byte[] bytes = message.getBytes("UTF-8");
                byte ID = 7;
                ByteBuf byteBuf = NettyMessage.allocateBuffer(UnpooledByteBufAllocator.DEFAULT,ID,0,4+message.length(),true);
                byteBuf.writeInt(bytes.length);
                byteBuf.writeBytes(bytes);
                embeddedChannel.writeInbound(byteBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMethod2_1(){
        ChannelInitializer channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            @Override
            public void initChannel(EmbeddedChannel ch)
                    throws Exception {
                ch.pipeline().addLast(messageDecoder);
//                ch.pipeline().addLast(new StringDecoder());
//                ch.pipeline().addLast(new StringProcessHandler());
            }
        };
        try {
            EmbeddedChannel embeddedChannel = new EmbeddedChannel(channelInitializer);
            for (int i = 0; i < 10; i++) {
                long a = 11 + i;
                byte ID = 7;
                ByteBuf byteBuf = NettyMessage.allocateBuffer(UnpooledByteBufAllocator.DEFAULT,ID,0,16,true);
//                byteBuf.writeInt(16);
                byteBuf.writeLong(Long.MAX_VALUE);
//                ByteBuf byteBuf = NettyMessage.allocateBuffer(UnpooledByteBufAllocator.DEFAULT,ID,0,4,true);
//                byteBuf.writeInt(1111);
                embeddedChannel.writeInbound(byteBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMethod3(){
        ChannelInitializer channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            @Override
            public void initChannel(EmbeddedChannel ch)
                    throws Exception {
//                ch.pipeline().addLast(new NettyMessage.NettyMessageDecoder(true));
                ch.pipeline().addLast(messageDecoder);
//                ch.pipeline().addLast(new StringDecoder());
//                ch.pipeline().addLast(new StringProcessHandler());
            }
        };
        try {
            EmbeddedChannel embeddedChannel = new EmbeddedChannel(channelInitializer);
            for (int i = 0; i < 100; i++) {
                NettyMessage.TestRequest testRequest = new NettyMessage.TestRequest(1000000000+i,100009999+i);
                ByteBuf byteBuf = testRequest.write(UnpooledByteBufAllocator.DEFAULT);
                embeddedChannel.writeInbound(byteBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {
        private final boolean restoreOldNettyBehaviour;

        NettyMessageDecoder(boolean restoreOldNettyBehaviour) {
            super(Integer.MAX_VALUE, 0, 4, -4, 4);
            this.restoreOldNettyBehaviour = restoreOldNettyBehaviour;
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            ByteBuf msg = (ByteBuf) super.decode(ctx, in);
            if (msg == null) {
                return null;
            }

            try {
                int magicNumber = msg.readInt();

                if (magicNumber != MAGIC_NUMBER) {
                    throw new IllegalStateException(
                            "Network stream corrupted: received incorrect magic number.");
                }

                byte msgId = msg.readByte();
//                int length = msg.readInt();
                System.out.println(msgId);
                System.out.println(msg.readLong());
//                System.out.println(msg.readInt());
//                System.out.println(msg.readLong());
//                System.out.println(length);
                return msg;
            } finally {
                // ByteToMessageDecoder cleanup (only the BufferResponse holds on to the decoded
                // msg but already retain()s the buffer once)
                msg.release();
            }
        }
    }
}
