package com.study.akka;

import akka.actor.AbstractActorWithStash;

/**
 * @author wzj
 * @date 2021/05/12
 */
public class ActorWithProtocol extends AbstractActorWithStash {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "open",
                        s-> {
                            getContext()
                                    .become(
                                            receiveBuilder()
                                                    .matchEquals(
                                                        "write",
                                                        ws -> {
                                                            System.out.println("do writing");
                                                        })
                                                    .matchEquals(
                                                            "close",
                                                            cs -> {
                                                                // 调用unstashAll()将消息从stash排队到 Actor 的邮箱，直到达到邮箱的容量（如果有），
                                                                // 请注意，stash中的消息是预先发送到邮箱的。如果有界邮箱溢出，
                                                                // 将引发MessageQueueAppendFailedException。调用unstashAll()后，stash保证为空。
                                                                unstashAll();
                                                                getContext().unbecome();
                                                            })
                                                    // 调用stash()会将当前消息（Actor 最后收到的消息）添加到 Actor 的stash中。
                                                    // 它通常在处理 Actor 消息处理程序中的默认情况时调用，以存储其他情况未处理的消息。
                                                    // 将同一条消息存储两次是非法的；这样做会导致IllegalStateException。
                                                    // stash也可以是有界的，在这种情况下，调用stash()可能导致容量冲突，
                                                    // 从而导致StashOverflowException。可以使用邮箱配置的stash-capacity设置（一个int值）存储容量。
                                                    .matchAny(msg -> stash())
                                                    .build(),
                                            false);
                        })
                .matchAny(msg -> stash())
                .build();
    }
}
