package com.study.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Optional;

/**
 * @author wzj
 * @date 2021/03/12
 */
public class Device extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(super.getContext().getSystem(), this);

    final String groupId;

    final String deviceId;

    Optional<Double> lastTemperatureReading = Optional.empty();

    public Device(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    public static Props props(String groupId, String deviceId){
        return Props.create(Device.class, () -> new Device(groupId, deviceId));
    }

    @Override
    public void preStart() throws Exception, Exception {
        log.info("Device actor {}-{} started", groupId, deviceId);
    }

    @Override
    public void postStop() throws Exception, Exception {
        log.info("Device actor {}-{} stopped", groupId, deviceId);
    }

    // 读消息和写消息
    public static final class RecordTemperature {
        final long requestId;
        final double value;

        public RecordTemperature(long requestId, double value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    public static final class TemperatureRecorded {
        final long requestId;

        public TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }

        public long getRequestId() {
            return requestId;
        }
    }

    // 使用设备组
    public static final class RequestTrackDevice {
        public final String groupId;
        public final String deviceId;

        public RequestTrackDevice(String groupId, String deviceId) {
            this.groupId = groupId;
            this.deviceId = deviceId;
        }
    }

    public static final class DeviceRegistered {
        public DeviceRegistered(){}
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, r -> {
                    if (this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)){
                        super.getSender().tell(new DeviceRegistered(), getSelf());
                    } else {
                        log.warning("Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
                                r.groupId, r.deviceId, this.groupId, this.deviceId);
                    }
                })
                .match(DeviceManager.RequestTrackDevice.class, r->{
                    if (this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)){
                        super.getSender().tell(new DeviceManager.DeviceRegisted(), getSelf());
                    } else {
                        log.warning("Ignoring DeviceManager.TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
                                r.groupId, r.deviceId, this.groupId, this.deviceId);
                    }
                })
                .match(RecordTemperature.class, r -> {
                    log.info("Recorded temperature reading {} with {}", r.value, r.requestId);
                    lastTemperatureReading = Optional.of(r.value);
                    super.getSender().tell(new TemperatureRecorded(r.requestId), super.getSelf());
                })
                .match(ReadTemperature.class, r -> {
                    log.info("ReadTemperature requestId {} <", r.requestId);
                    super.getSender().tell(new ResponseTemperature(r.requestId, lastTemperatureReading), super.getSelf());
                })
                .build();
    }
}
