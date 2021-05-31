package com.study.agent;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author wzj
 * @date 2021/05/31
 */
public class Transformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null){
            //返回null，将会使用原生class。
            return null;
        }
        if (className.startsWith("java") ||
                className.startsWith("javax") ||
                className.startsWith("jdk") ||
                className.startsWith("sun") ||
                className.startsWith("com/sun") ||
                className.startsWith("com/intellij") ||
                className.startsWith("org/jetbrains") ||
                className.startsWith("com/study/agent") ||
                className.startsWith("com/thoreauz/agent")
        ){
            // 不对JDK类以及agent类增强
            return null;
        }
        //读取类的字节码流
        ClassReader reader = new ClassReader(classfileBuffer);
        //创建操作字节流值对象，ClassWriter.COMPUTE_MAXS:表示自动计算栈大小
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        //接收一个ClassVisitor子类进行字节码修改
        reader.accept(new TimeClassVisitor(writer, className), 8);
        //返回修改后的字节码流
        return writer.toByteArray();
    }
}
