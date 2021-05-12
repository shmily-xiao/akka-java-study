package com.study.akka;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * 使用become的方法不是替换而是添加到行为堆栈的顶部。在这种情况下，必须注意确保pop操作的数量（即unbecome）与push操作的数量在长期内匹配，否则这将导致内存泄漏，这就是为什么此行为不是默认行为。
 * @author wzj
 * @date 2021/05/12
 */
public class Swapper extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals(
                Swapper.class,
                s -> {
                    log().info("Hi");
                    getContext().become(
                            receiveBuilder()
                            .matchEquals(
                                    Swapper.class,
                                    x -> {
                                        log().info("Ho");
                                        getContext().unbecome();
                                    }
                            ).build(),
                            false);
                })
                .build();
    }
}

class SwapperApp{
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("SwapperSystem");
        ActorRef swapper = system.actorOf(Props.create(Swapper.class), "swapper");
        swapper.tell(Swapper.class, ActorRef.noSender()); // Hi
        swapper.tell(Swapper.class, ActorRef.noSender()); // Ho
        swapper.tell(Swapper.class, ActorRef.noSender()); // Hi
        swapper.tell(Swapper.class, ActorRef.noSender()); // Ho
        swapper.tell(Swapper.class, ActorRef.noSender()); // Hi
        swapper.tell(Swapper.class, ActorRef.noSender()); // Ho
        system.terminate();
    }
}