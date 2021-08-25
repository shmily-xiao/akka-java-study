package com.study.akka.stream;


import akka.actor.typed.ActorRef;
import akka.japi.function.Function;
import akka.japi.JavaPartialFunction;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorSource;

import java.util.Optional;

public class StreamTest{

    interface Protocol{}

    class Message implements Protocol{
        private final String msg;

        public Message(String msg) {
            this.msg = msg;
        }
    }

    class Complete implements Protocol{}

    class Fail implements Protocol{
        private final Exception ex;

        public Fail(Exception ex) {
            this.ex = ex;
        }
    }

    public static void main(String[] args) {
        final Function<Protocol, Optional<Throwable>> failureMatcher = new Function<Protocol, Optional<Throwable>>() {
            @Override
            public Optional<Throwable> apply(Protocol param) throws Exception {
                if (param instanceof  Fail) {
                    return Optional.of(((Fail) param).ex);
                }
                return Optional.empty();
            }
        };
//                new JavaPartialFunction<Protocol, Throwable>() {
//                    @Override
//                    public Throwable apply(Protocol x, boolean isCheck) throws Exception {
//                        if (x instanceof  Fail){
//                            return ( (Fail) x).ex;
//                        }else {
//                            throw noMatch();
//                        }
//                    }
//                };
        final Source<Protocol, ActorRef<Protocol>> source
                // 有反压
                = ActorSource.actorRef( (m) -> m instanceof Complete, failureMatcher, 8, OverflowStrategy.fail());

        RunnableGraph<ActorRef<Protocol>> graph = source.collect(
                new JavaPartialFunction<Protocol, String>() {
                    public String apply(Protocol p, boolean isCheck) {
                        if (p instanceof Message) {
                            return ((Message) p).msg;
                        } else {
                            throw noMatch();
                        }
                    }
                })
                .to(Sink.foreach(System.out::println));
    }

}


