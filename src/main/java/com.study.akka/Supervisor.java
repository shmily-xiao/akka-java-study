package com.study.akka;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;

import java.time.Duration;

/**
 * @author wzj
 * @date 2021/05/13
 */
public class Supervisor extends AbstractActor {

    private static SupervisorStrategy strategy = new OneForOneStrategy(
            10,
            Duration.ofMinutes(1), // 这意味着策略每分钟重新启动一个子级最多10次
            DeciderBuilder.match(ArithmeticException.class, e -> {
                System.out.println("-------> resume <---------");
                e.printStackTrace();
                return SupervisorStrategy.resume();
            })
            .match(NullPointerException.class, e -> {
                System.out.println("--------> restart <---------");
                e.printStackTrace();
                return SupervisorStrategy.restart();
            })
            .match(IllegalArgumentException.class, e -> {
                System.out.println("--------> stop <--------");
                e.printStackTrace();
                return SupervisorStrategy.stop();
            })
            .matchAny(o -> {
                System.out.println("--------> escalate <--------");
                o.printStackTrace();
                return SupervisorStrategy.escalate();
            })
            .build()
            );
    @Override
    public SupervisorStrategy supervisorStrategy(){
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Props.class,
                        props -> {
                            getSender().tell(getContext().actorOf(props), getSelf());
                        })
                .build();
    }
}


class Child extends AbstractActor{
    int state = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Exception.class,
                        exception -> {throw exception;})
                .match(Integer.class, i -> state = i)
                .matchEquals("get", s -> getSender().tell(state, getSelf()))
                .build();
    }
}


