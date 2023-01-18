package com.study.akka.serializer;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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
                .match(PBCustomer.Customer.class, message -> {
                    System.out.println("PBCustomer.Customer");
                })
                .match(TestWithSerialConfigByJava.class, message -> {
                    System.out.println("TestWithSerialConfigByJava");
                })
                .match(TestWithSerialConfigByProto.class, message -> {
                    System.out.println("TestWithSerialConfigByProto");
                })
                .match(String.class, message -> {
                    System.out.println("String");
                })
                .matchAny(message -> {
                    System.out.println("matchany");
                    System.out.println(message);
                })
                .build();
    }

    /**
     * 这个案例只是测试 akka 的序列化和反序列化的原理，并不完整。
     * @param args
     */
    public static void main(String[] args) throws Exception{

        final Config config = ConfigFactory.parseURL(TestAkkaSerial.class.getClassLoader().getResource("application.conf"));

        ActorSystem system = ActorSystem.create("test-system-server", config);

        Serialization serialization = SerializationExtension.get(system);

        Serializer customer_serializer = serialization.serializerFor(PBCustomer.Customer.class);
        Serializer testWithSerialConfigByJava_serializer = serialization.serializerFor(TestWithSerialConfigByJava.class);
        Serializer testWithSerialConfigByProto_serializer = serialization.serializerFor(TestWithSerialConfigByProto.class);

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
