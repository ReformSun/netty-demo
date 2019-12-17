package com.sunny.java.nio.buffer;

import java.nio.CharBuffer;

public class TestCharBuffer {
    public static void main(String[] args) {

    }

    public static void testMethod1(){
        CharBuffer charBuffer = CharBuffer.allocate(8);
        charBuffer.append("ddd");
    }

    public static void testMethod2(){
        CharBuffer charBuffer = CharBuffer.allocate(8);
        charBuffer.append('c');
        String a = "cc";
    }
}
