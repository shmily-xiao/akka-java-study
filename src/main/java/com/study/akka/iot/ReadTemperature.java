package com.study.akka.iot;

public final class ReadTemperature {
    final long requestId;

    public ReadTemperature(long requestId){
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }
}
