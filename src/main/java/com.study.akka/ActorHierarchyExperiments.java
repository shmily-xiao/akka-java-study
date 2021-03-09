package com.study.akka;

import akka.actor.*;
import scala.Option;

import java.io.IOException;

/**
 * @author wzj
 * @date 2021/03/09
 */
public class ActorHierarchyExperiments {
    public static void main(String[] args) throws IOException{
        ActorSystem testSystem = ActorSystem.create("testSystem");
//        ActorRef firstActorRef = testSystem.actorOf(PrintMyActorRefActor.props(), "first-actor");
//        System.out.println("First: " + firstActorRef);
//
////        firstActorRef.tell("printit", firstActorRef);
//        firstActorRef.tell("printit", ActorRef.noSender());


        ActorRef supervisingActor = testSystem.actorOf(SupervisingActor.props(), "supervising-actor");
        supervisingActor.tell("failChild", ActorRef.noSender());



        System.out.println(">>> enter to end >>>>");
        try{
            System.in.read();
        } finally {
            testSystem.terminate();
        }
    }
}

class PrintMyActorRefActor extends AbstractActor{

    static Props props(){
        return Props.create(PrintMyActorRefActor.class, PrintMyActorRefActor::new);
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .matchEquals("printit", p->{
                    ActorRef actorRef = getContext().actorOf(Props.empty(), "second-actor");
                    System.out.println("Second: " + actorRef);
                    System.out.println("'" + p +"'");
                })
                .build();
    }
}


class SupervisingActor extends AbstractActor{

    static Props props(){
        return Props.create(SupervisingActor.class, SupervisingActor::new);
    }

    ActorRef child = getContext().actorOf(SupervisedActor.props(), "supervised-actor");

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .matchEquals("failChild", f -> {
                    child.tell("fail", getSelf());
                })
                .build();
    }
}


class SupervisedActor extends AbstractActor{

    static Props props(){
        return Props.create(SupervisedActor.class, SupervisedActor::new);
    }

    @Override
    public void preStart() throws Exception, Exception {
        System.out.println("supervisedActor  actor started");
    }

    @Override
    public void postStop() throws Exception, Exception {
      System.out.println("supervisedActor is stopped");
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception, Exception {
        System.out.println("supervisedActor restart");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("fail", f -> {
                    System.out.println("supervised actor fails now");
                    throw new Exception("I failed");
                })
                .build();
    }
}
