package com.study.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

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
    // 记录注册信息
    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();

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
                // 存在自己的map中
                deviceIdToActor.put(trackMsg.deviceId, deviceActor);
                // 将消息发出去，保留原始的发送者
                deviceActor.forward(trackMsg, getContext());
                // forward 等同于 下面的这句话
//                 deviceActor.tell(trackMsg, getContext().getSender());

//                 deviceActor.tell(trackMsg, getSelf());
            }
        }else {
            log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.", trackMsg.groupId, this.groupId);
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Device.RequestTrackDevice.class, this::onTrackDevice)
                .build();
    }
}
