package com.sunny.coding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class TestByteBufUtil {
    public static void main(String[] args) {
        
    }
    
    public static void testMethod1(){
        // 使用ByteBufAllocator进行分配缓冲区
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        CharBuffer charBuffer = CharBuffer.allocate(8);
        charBuffer.append("cccccccc");
        // 使用utf8进行编码
        Charset charset = Charset.forName("utf8");
        ByteBuf byteBuf = ByteBufUtil.encodeString(byteBufAllocator,charBuffer,charset);


    }
}
