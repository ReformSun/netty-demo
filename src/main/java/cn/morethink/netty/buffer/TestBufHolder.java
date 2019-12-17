package cn.morethink.netty.buffer;

import io.netty.buffer.*;

public class TestBufHolder {
    public static void main(String[] args) {
        
    }
    
    public static void testMethod1(){
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf byteBuf = byteBufAllocator.directBuffer(100);
        ByteBufHolder byteBufHolder = new DefaultByteBufHolder(byteBuf);
    }
}
