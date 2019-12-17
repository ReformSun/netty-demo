package com.sunny.coding;

public class TestStringCoding {
    public static void main(String[] args) {
//        testMethod1();
        testMethod2();
    }

    public static void testMethod1(){
        String s = "dddddd";
        System.out.println(s.length());
        byte[] bytes = s.getBytes();
        System.out.println(bytes.length);
        System.out.println((char) bytes[0]);
//        System.out.println('d');
    }

    public static void testMethod2(){
        String s = "你好";
        System.out.println(s.length());
        byte[] bytes = s.getBytes();
        System.out.println(bytes.length);
        System.out.println(bytes[0]);
//        System.out.println('d');
    }
}
