package com.study.agent;

public class Application {
    public static void main(String[] args) throws InterruptedException{
        System.out.println("hello world!");
        add(1,2);
        System.out.println("hello world!");
    }

    private static int add(int a, int b) throws InterruptedException{
        Thread.sleep(3000);
        return a+b;
    }
}
