package com.study.akka.iot;

import java.util.Optional;

public class ResponseTemperature {
    final long requestIdl;

    final Optional<Double> value;

    public ResponseTemperature(long requestIdl, Optional<Double> value) {
        this.requestIdl = requestIdl;
        this.value = value;
    }
}
