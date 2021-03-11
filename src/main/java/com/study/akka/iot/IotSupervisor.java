package com.study.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author wzj
 * @date 2021/03/10
 */
public class IotSupervisor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(super.getContext().getSystem(), this);

    public static Props props(){
        return Props.create(IotSupervisor.class, IotSupervisor::new);
    }

    @Override
    public void preStart() throws Exception, Exception {
        log.info("Iot Application started");
    }

    @Override
    public void postStop() throws Exception, Exception {
        log.info("Iot application stopped");
    }

    @Override
    public Receive createReceive() {
        return super.receiveBuilder().build();
    }
}
