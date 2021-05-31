package com.study.agent;

import java.util.HashMap;
import java.util.Map;

public class TimeCache {
    public static Map<String, Long> startTimeMap = new HashMap<>();

    public static Map<String, Long> endTimeMap = new HashMap<>();

    public static void setStartTimeMap(String methodName, long time){
        startTimeMap.put(methodName, time);
    }

    public static void setEndTimeMap(String methodName, long time){
        endTimeMap.put(methodName, time);
    }

    public static String getCostTime(String methodName){
        long start = startTimeMap.get(methodName);
        long end = endTimeMap.get(methodName);
        return methodName + "[" + (end - start) + " ms]";
    }
}
