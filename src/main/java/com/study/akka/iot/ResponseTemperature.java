package com.study.akka.iot;

import java.util.Optional;

public class ResponseTemperature {
    final long requestId;

    final Optional<Double> value;

    public ResponseTemperature(long requestId, Optional<Double> value) {
        this.requestId = requestId;
        this.value = value;
    }

    public long getRequestId() {
        return requestId;
    }

    public Optional<Double> getValue() {
        return value;
    }
}
