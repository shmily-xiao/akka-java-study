package com.study.agent;

import java.lang.instrument.Instrumentation;

/**
 * @author wzj
 * @date 2021/05/31
 */
public class MyAgent {
    //代理程序入口函数
    public static void premain(String args, Instrumentation inst) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.out.println("agent premain begin");
        //添加字节码转换器
        inst.addTransformer(new Transformer(), true);
        System.out.println("agent premain end");
    }
}
