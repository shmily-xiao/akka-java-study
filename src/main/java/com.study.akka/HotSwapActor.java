package com.study.akka;

import akka.actor.AbstractActor;

/**
 * 实现有限状态机（FSM，例如「Dining Hakkers」）。它将替换当前行为（即行为堆栈的顶部），这意味着你不使用unbecome，而是始终显式安装下一个行为。
 * @author wzj
 * @date 2021/05/12
 */
public class HotSwapActor extends AbstractActor {
    private AbstractActor.Receive angry;
    private AbstractActor.Receive happy;

    public HotSwapActor() {
        angry = receiveBuilder()
                .matchEquals(
                        "foo",
                        s -> {
                            getSender().tell("I am already angry?", getSelf());
                        })
                .matchEquals(
                        "bar",
                        s -> {
                            getContext().become(happy);
                        })
                .build();
        happy = receiveBuilder()
                .matchEquals(
                        "bar",
                        s -> {
                            getSender().tell("I am already happy :-)", getSelf());
                        })
                .matchEquals(
                        "foo",
                        s -> {
                            getContext().become(angry);
                        })
                .build();
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("foo", s -> getContext().become(angry))
                .matchEquals("bar", s -> getContext().become(happy))
                .build();
    }
}
