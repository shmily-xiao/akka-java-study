package com.study.akka.myapp;

import akka.actor.*;

/**
 * 暂时没有通
 */
public class Client extends AbstractActor {
    static Props props() {
        System.out.println("client init");
        return Props.create(Client.class, Client::new);
    }

    @Override
    public Receive createReceive() {
        System.out.println("client createReceive in");
        return receiveBuilder()
                .matchEquals("get", p -> {
                    ActorSelection selection = getContext().actorSelection("akka://test-system-server@127.0.0.1:25520/user/test-supervisor-server");
                    selection.tell("printit", getSelf());
                })
                .build();
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("test-system-client");
        try {
            // Create top level supervisor
            ActorRef supervisor = system.actorOf(Client.props(), "test-supervisor-client");

            System.out.println("Press ENTER to exit the system");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }
}