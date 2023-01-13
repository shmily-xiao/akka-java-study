package com.study.akka.myapp.server;

import akka.actor.*;

import java.util.Optional;

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
                    ActorRef secondRef;
                    Optional<ActorRef> child = getContext().findChild("server-response-actor");
                    secondRef = child.orElseGet(() -> getContext().actorOf(Props.empty(), "server-response-actor"));
                    System.out.println("Second: " + secondRef);
                    System.out.println("server receive: " + p);
                    getSender().tell("i print it success", secondRef);
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
