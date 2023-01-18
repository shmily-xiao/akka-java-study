package com.study.akka.serializer;

/**
 * @author wzj
 * @date 2023/01/17
 */
public class TestWithSerialConfigByProto {
    private String nameProto;

    private Integer ageProto;


    public TestWithSerialConfigByProto() {
    }

    public TestWithSerialConfigByProto(String nameProto, Integer ageProto) {
        this.nameProto = nameProto;
        this.ageProto = ageProto;
    }

    public Integer getAgeProto() {
        return ageProto;
    }

    public void setAgeProto(Integer ageProto) {
        this.ageProto = ageProto;
    }

    public String getNameProto() {
        return nameProto;
    }

    public void setNameProto(String nameProto) {
        this.nameProto = nameProto;
    }
}
