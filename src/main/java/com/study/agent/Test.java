package com.study.agent;

public class Test {
    public void test(){
        TimeCache.setStartTimeMap("test", System.currentTimeMillis());

        TimeCache.setEndTimeMap("test", System.currentTimeMillis());

        System.out.println(TimeCache.getCostTime("test"));
    }
}
