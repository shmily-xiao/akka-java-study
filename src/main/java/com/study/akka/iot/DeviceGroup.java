package com.study.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author wzj
 * @date 2021/03/17
 */
 public class DeviceGroup extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(super.getContext().getSystem(), this);

    final String groupId;

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props(String groupId){
        return Props.create(DeviceGroup.class, () -> new DeviceGroup(groupId));
    }

    /**
     * 新的查询功能
     */
    public static final class RequestDeviceList{
        final long requestId;

        public RequestDeviceList(long requestId) {
            this.requestId = requestId;
        }
    }

    /**
     * 回复信息
     */
    public static final class ReplyDeviceList{
        public final long requestId;
        public final Set<String > ids;

        public ReplyDeviceList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }


    /**
     *  记录注册信息
     */
    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    /**
     * 反向记录信息
     */
    final Map<ActorRef, String> actorToDeviceId = new HashMap<>();

    @Override
    public void preStart() throws Exception, Exception {
        log.info("deviceGroup {} started", groupId);
    }

    @Override
    public void postStop() throws Exception, Exception {
        log.info("deviceGroup {} stopped", groupId);
    }


    private void onTrackDevice(Device.RequestTrackDevice trackMsg){
        if (this.groupId.equals(trackMsg.groupId)){
            ActorRef deviceActor = deviceIdToActor.get(trackMsg.deviceId);
            if(deviceActor != null){
                // 两者之间的唯一区别是，forward保留原始发送者，而tell将发送者设置为当前 Actor。
                // 就像我们的设备 Actor 一样，我们确保不响应错误的组 ID
                deviceActor.forward(trackMsg, getContext());
            } else {
                log.info("Creating device actor for {}", trackMsg.deviceId);
                // 创建 actor
                deviceActor = super.getContext().actorOf(Device.props(groupId, trackMsg.deviceId), "device-"+trackMsg.deviceId);

                // watch
                // 当新设备 Actor 被创建时开始观察（watching）。
                getContext().watch(deviceActor);

                // 存在自己的map中
                deviceIdToActor.put(trackMsg.deviceId, deviceActor);
                actorToDeviceId.put(deviceActor, trackMsg.deviceId);
                // 将消息发出去，保留原始的发送者
                deviceActor.forward(trackMsg, getContext());
                // forward 等同于 下面的这句话
//                 deviceActor.tell(trackMsg, getContext().getSender());
            }
        }else {
            log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.", trackMsg.groupId, this.groupId);
        }
    }

    /**
     * 查询有哪些deviceid
     * @param r
     */
    private void onDeviceList(RequestDeviceList r){
        log.info("onDeviceList {} ", r.requestId);
        log.info("deviceIdToActor.keySet() {} ", deviceIdToActor.keySet());
        super.getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }

    /**
     * 如果出现问题就删除注册表
     */
    private void onTerminated(Terminated t){
        ActorRef deviceActor = t.getActor();
        String deviceId = actorToDeviceId.get(deviceActor);
        log.info("Device actor for {} has been terminated", deviceId);
        actorToDeviceId.remove(deviceActor);
        deviceIdToActor.remove(deviceId);
    }

    public static final class RequestAllTemperatures{
        final long requestId;

        public RequestAllTemperatures(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class RespondAllTemperatures{
        final long requestId;
        final Map<String,TemperatureReading> temperatures;

        public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }

    public static interface TemperatureReading{
    }

    public static final class Temperature implements TemperatureReading{
        public final double value;

        public Temperature(double value) {
            this.value = value;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Temperature that = (Temperature) o;

            return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            long temp = Double.doubleToLongBits(value);
            return (int) (temp ^ (temp >>> 32));
        }

        @Override
        public String toString() {
            return "Temperature{" +
                    "value=" + value +
                    '}';
        }
    }
    public enum TemperatureNotAvailable implements TemperatureReading{
        INSTANCE
    }

    public enum DeviceNotAvailable implements TemperatureReading{
        INSTANCE
    }

    public enum DeviceTimeOut implements TemperatureReading{
        INSTANCE
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Device.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }
}
