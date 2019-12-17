package com.sunny.coding;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class TestCharset {
    public static void main(String[] args) {
        
    }
    
    public static void testMethod1(){
        //返回指定的字符集CharSet
        Charset charset = Charset.forName("utf8");
        CharBuffer charBuffer = CharBuffer.allocate(8);
        charBuffer.append('a');
        ByteBuffer byteBuffer = encoder(charset,charBuffer);
        System.out.println(byteBuffer);



    }

    public static void testMethod2(){
        //返回虚拟机默认的字符集CharSet
        Charset charset1 = Charset.defaultCharset();
    }


    /**
     * 对char进行编码
     * @param charset
     * @param in
     */
    public static ByteBuffer encoder(Charset charset, CharBuffer in){
        CharsetEncoder encoder = charset.newEncoder();
        try {
            ByteBuffer bytebuffer = encoder.encode(in);
            return bytebuffer;
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对byte进行解码
     * @param charset
     * @param in
     */
    public static CharBuffer decoder(Charset charset, ByteBuffer in){
        CharsetDecoder decoder = charset.newDecoder();
        try {
            CharBuffer charBuffer = decoder.decode(in);
            return charBuffer;
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
