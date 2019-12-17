package com.sunny.java.nio.buffer;

import java.nio.ByteBuffer;

/**
 * java 提供的nio buffer
 * https://www.cnblogs.com/lxzh/archive/2013/05/10/3071680.html
 *
 * mark
 * position
 * limit
 * capacity
 * address
 *
 * 确定：
 * 1. 长度固定，一旦分配完成，它的容量不能动态扩展和收缩，当需要编码的pojo对象大于容量时，会发生索引越界异常
 * 2. 只有一个表示位置的指针postiton，读写的时候需要手工调用flip
 * 3. 接口功能有限
 *
 */
public class TestBuffer {
    public static void main(String[] args) {
//        testMethod1();
        testMethod2();
    }

    /**
     * 写数据
     * 写完数据之后可以发现：
     * position = 8
     * limit = 88
     * remaining = limit - position = 80
     *
     * flip 后
     *
     * position = 0
     * limit = 8
     * remaining = limit - position = 8
     *
     * 发现flip的作用 是写入的下一个位置为0
     *
     * 之后直接读 不能直接读，读取的数据的位置是
     *
     *
     */
    public static void testMethod1(){
        ByteBuffer buffer = ByteBuffer.allocate(88);
        String value = "jjkkllll";
        print(buffer);
        System.out.println("写入数据长度1：" + value.length());
        System.out.println("写入数据长度：" + value.getBytes().length);
        buffer.put(value.getBytes());
        print(buffer);
        buffer.flip();
        print(buffer);

    }

    /**
     * 不进行flip操作
     * 知道写入的长度
     * 读取写入的长度
     * 发现读出的数据不是我们写入的数据
     * 通过源码发现遍历调用调用了get方法获取字段信息
     * 并且get方法是返回是position位置的字节信息
     * 那我们可以通过把position位置设为0就可以读取写入的信息了
     * 发现是可以获取写入的数据
     */
    public static void testMethod2(){
        ByteBuffer buffer = ByteBuffer.allocate(88);
        String value = "jjkkllll";
        buffer.put(value.getBytes());
        byte[] vbytes = new byte[value.getBytes().length];
        buffer.get(vbytes);
        System.out.println(new String(vbytes));
        // 发现position变为了16说明每调用get方法一次，position就会自动加1
        System.out.println(buffer.position());
        // 修改position为0
        buffer.position(0);
        buffer.get(vbytes);
        System.out.println(new String(vbytes));
    }

    /**
     * 怎么有效的读取缓存中已经写入的数据那
     * 在不知道写到哪里的情况下
     * 通过调用flip方法
     * limit = position; 设置当前位置给limit
     * position = 0; 设置当前位置为0
     * mark = -1;
     *  当调用remaining方法时，会得到limit - position的长度，也就是写入内容的长度
     *  这时再去读取就可以获取全部缓冲区写入内容
     *
     * @param byteBuffer
     */
    public static void readAndPrint(ByteBuffer byteBuffer){
        byteBuffer.flip();
//        byteBuffer.rewind();
        byte[] vbytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(vbytes);
        System.out.println(new String(vbytes));
    }



    private static void print(ByteBuffer buffer){
        System.out.println("start");
        System.out.println("位置信息：" + buffer.position());
//        System.out.println("标记位置信息：" + buffer.mark());
        System.out.println("剩余的位置" + buffer.remaining());
        System.out.println("限制数：" + buffer.limit());
        System.out.println("end");
    }
}
