package com.study.akka.myapp.server;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Server extends AbstractLoggingActor {
    static Props props() {
        System.out.println("Server init");
        return Props.create(Server.class, Server::new);
    }
    @Override
    public Receive createReceive() {
        super.log().info("createReceive method in");
        return receiveBuilder()
                .matchEquals("printit", p -> {
                    ActorRef secondRef = getContext().actorOf(Props.empty(), "second-actor");
                    System.out.println("Second: " + secondRef);
                })
                .build();
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("test-system-server");
        try {
            // Create top level supervisor
            ActorRef supervisor = system.actorOf(Server.props(), "test-supervisor-server");

            System.out.println("Press ENTER to exit the system");
            System.in.read();
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            system.terminate();
        }
    }
}
