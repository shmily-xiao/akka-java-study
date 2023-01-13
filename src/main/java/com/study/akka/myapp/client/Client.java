package com.study.akka.myapp.client;

import akka.actor.*;

/**
 * 启动client的时候需要去修改一下 application.conf 的 port ，让其和server的端口不一致
 */
public class Client extends AbstractActor {
    static Props props() {
        System.out.println("client init");
        Props props = Props.create(Client.class, Client::new);
        return props;
    }

    @Override
    public Receive createReceive() {
        System.out.println("client createReceive in");
        return receiveBuilder()
                .matchEquals("get", p -> {
                    System.out.println("client get processor! get--》");
                    ActorSelection selection = getContext().actorSelection("akka://test-system-server@127.0.0.1:25520/user/test-supervisor-server");
                    selection.tell("printit", getSelf());
                })
                .matchEquals("i print it success", p -> {
                    System.out.println("client get processor! --》 i print it success");
                    System.out.println("client get the sender is: " + sender());
                })
                .build();
    }

    public static void main(String[] args) {
        // 启动client的时候需要去修改一下 application.conf 的 port ，让其和server的端口不一致
        // server 用的是 25520
        // client 用的是 25521
        ActorSystem system = ActorSystem.create("test-system-client");
        try {
            // Create top level supervisor
            ActorRef supervisor = system.actorOf(Client.props(), "test-supervisor-client");

            for (int i = 0; i< 10; i++) {
                supervisor.tell("get", supervisor);
                Thread.sleep(5000);
            }

            System.out.println("Press ENTER to exit the system");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }
}