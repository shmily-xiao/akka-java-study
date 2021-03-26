package com.study.akka.iot;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wzj
 * @date 2021/03/26
 */
public class DeviceGroupQuery extends AbstractActor {

    public static final class CollectionTimeout{}

    private final LoggingAdapter log = Logging.getLogger(super.getContext().getSystem(), this);

    final Map<ActorRef, String> actorToDeviceId;
    final long requestId;
    final ActorRef requester;
    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
        this.actorToDeviceId = actorToDeviceId;
        this.requestId = requestId;
        this.requester = requester;

        queryTimeoutTimer = super.getContext().getSystem().scheduler().scheduleOnce(timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
    }

    public static Props props(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout){
        return Props.create(DeviceGroupQuery.class, () -> new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout));
    }

    @Override
    public void preStart() throws Exception, Exception {
        for (ActorRef deviceActor : actorToDeviceId.keySet()){
            super.getContext().watch(deviceActor);
            // 开始就发一个消息
            deviceActor.tell(new ReadTemperature(0L), super.getSelf());
        }
    }

    @Override
    public void postStop() throws Exception, Exception {
        queryTimeoutTimer.cancel();
    }

    @Override
    public Receive createReceive() {

        return waitingForReplies(new HashMap<>(), actorToDeviceId.keySet());
    }

    public void receiveResponse(ActorRef deviceActor, DeviceGroup.TemperatureReading reading, Set<ActorRef> stillWaiting,
                                Map<String, DeviceGroup.TemperatureReading> repliesSoFar){
        super.getContext().unwatch(deviceActor);
        String deviceId = actorToDeviceId.get(deviceActor);

        HashSet<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
        newStillWaiting.remove(deviceActor);

        Map<String, DeviceGroup.TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
        newRepliesSoFar.put(deviceId, reading);

        if (newStillWaiting.isEmpty()){
            requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
            super.getContext().stop(getSelf());
        }else {
            getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
        }

    }

    /**
     * 柑橘有点像是轮转，从 stillwaiting 中轮转到 repliesSoFar 中。
     * @param repliesSoFar
     * @param stillWaiting
     * @return
     */
    public Receive waitingForReplies(Map<String, DeviceGroup.TemperatureReading> repliesSoFar,
                                     Set<ActorRef> stillWaiting){
        return receiveBuilder()
                .match(ResponseTemperature.class, r -> {
                    ActorRef deviceActor = getSender();
                    DeviceGroup.TemperatureReading reading  = r.value
                            .map(v -> (DeviceGroup.TemperatureReading)new DeviceGroup.Temperature(v))
                            .orElse(DeviceGroup.TemperatureNotAvailable.INSTANCE);
                    receiveResponse(deviceActor, reading, stillWaiting, repliesSoFar);
                })
                .match(Terminated.class, t -> {
                    log.info("waitingForReplies Terminated is running");
                    receiveResponse(t.getActor(), DeviceGroup.DeviceNotAvailable.INSTANCE, stillWaiting, repliesSoFar);
                })
                .match(CollectionTimeout.class, t -> {
                    log.info("waitingForReplies CollectionTimeout is running");
                    Map<String, DeviceGroup.TemperatureReading> replies = new HashMap<>(repliesSoFar);
                    for(ActorRef deviceActor : stillWaiting){
                        String deviceId = actorToDeviceId.get(deviceActor);
                        replies.put(deviceId, DeviceGroup.DeviceTimeOut.INSTANCE);
                    }
                    requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, replies), getSelf());
                    getContext().stop(getSelf());
                })
                .build();
    }
}
