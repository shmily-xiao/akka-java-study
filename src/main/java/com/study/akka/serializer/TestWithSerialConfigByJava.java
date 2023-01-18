package com.study.akka.serializer;

import java.io.Serializable;

/**
 * @author wzj
 * @date 2023/01/17
 */
public class TestWithSerialConfigByJava{
//    private static final long serialVersionUID = 111111L;
    private String nameJava;
    private int ageJava;

    public TestWithSerialConfigByJava() {
    }

    public TestWithSerialConfigByJava(String nameJava, int ageJava) {
        this.nameJava = nameJava;
        this.ageJava = ageJava;
    }

    public String getNameJava() {
        return nameJava;
    }

    public void setNameJava(String nameJava) {
        this.nameJava = nameJava;
    }

    public int getAgeJava() {
        return ageJava;
    }

    public void setAgeJava(int ageJava) {
        this.ageJava = ageJava;
    }
}
