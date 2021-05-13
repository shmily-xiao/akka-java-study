package com.study.akka;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * @author wzj
 * @date 2021/05/13
 */
public class Supervisor2 extends AbstractActor{

    private static SupervisorStrategy strategy = new OneForOneStrategy(
            10,
            Duration.ofMinutes(1), // 这意味着策略每分钟重新启动一个子级最多10次
            DeciderBuilder.match(ArithmeticException.class, e -> {
                System.out.println("-------> resume <---------");
                System.out.println(e.getMessage());
                return SupervisorStrategy.resume();
            })
                    .match(NullPointerException.class, e -> {
                        System.out.println("--------> restart <---------");
//                        e.printStackTrace();
                        System.out.println(e.getMessage());
                        return SupervisorStrategy.restart();
                    })
                    .match(IllegalArgumentException.class, e -> {
                        System.out.println("--------> stop <--------");
                        System.out.println(e.getMessage());
//                        e.printStackTrace();
                        return SupervisorStrategy.stop();
                    })
                    .matchAny(o -> {
                        System.out.println("--------> escalate <--------");
//                        o.printStackTrace();
                        System.out.println(o.getMessage());
                        return SupervisorStrategy.escalate();
                    })
                    .build()
    );

    @Override
    public SupervisorStrategy supervisorStrategy(){
        return strategy;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(
                        Props.class,
                        props -> {
                            getSender().tell(getContext().actorOf(props), getSelf());
                        })
                .build();
    }

    @Override
    public void preRestart(Throwable cause, Optional<Object> msg) {
        // do not kill all children, which is the default here
    }
}
