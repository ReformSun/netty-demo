package cn.morethink.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

public class TestBuf {
    public static void main(String[] args) {
//        testMethod1();
//        testMethod2();
        testMethod3();
    }
    
    public static void testMethod1(){
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf byteBuf = byteBufAllocator.directBuffer(100);
        byteBuf.writeBytes("sss".getBytes());

        byte[] bytes = new byte[3];
//        byteBuf.readBytes(bytes);

//        System.out.println(new String(bytes));


        String string1 = "deeee";
        byteBuf.writeBytes(string1.getBytes());
        bytes = new byte[string1.length()];
        byteBuf.readBytes(bytes);
        System.out.println(new String(bytes));
    }

    public static void testMethod2(){
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf byteBuf = byteBufAllocator.directBuffer(100);
        byteBuf.writeDouble(100);
        System.out.println(byteBuf.readDouble());
        byteBuf.writeInt(111);
        System.out.println(byteBuf.readInt());
    }

    /**
     * readableBytes()
     * 可读字节数
     */
    public static void testMethod3(){
        ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf byteBuf = byteBufAllocator.directBuffer(100);
        byteBuf.writeLong(1000);
        System.out.println(byteBuf.writerIndex() - byteBuf.readerIndex());
        System.out.println(byteBuf.readableBytes());
        System.out.println(byteBuf.readLong());

        byteBuf.writeInt(2);
        System.out.println(byteBuf.writerIndex() - byteBuf.readerIndex());
        System.out.println(byteBuf.readableBytes());
        System.out.println(byteBuf.readInt());
    }
    public static void testMethod4(){

    }
}
