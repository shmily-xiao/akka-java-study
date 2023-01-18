package com.study.akka.serializer;

import akka.actor.*;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import akka.serialization.SerializerWithStringManifest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * 本地调用
 *
 * @author wzj
 * @date 2023/01/13
 */
public class TestAkkaSerial {
    public static class ServerActor extends AbstractActor {

        private final Serializer serializer;

        public ServerActor(Serializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public Receive createReceive() {
            return super.receiveBuilder()
                    .match(byte[].class, message -> {
                        PBCustomer.Customer customer = (PBCustomer.Customer) serializer.fromBinary(message, PBCustomer.Customer.class);
                        PBCustomer.Customer newCustomer = PBCustomer.Customer.newBuilder()
                                .setAge(customer.getAge() + 1)
                                .setTel("my tell server")
                                .setName(customer.getName())
                                .build();
                        getSender().tell(serializer.toBinary(newCustomer), getSelf());
                    })
                    .match(String.class, message -> {
                        System.out.println(message);
                    })
                    .match(PBCustomer.Customer.class, message -> {
                        System.out.println(message.getName());
                    })
                    .build();
        }
    }

    public static class ClientActor extends AbstractActor {

        private final Serializer serializer;
        private final ActorRef actorRef;

        public ClientActor(Serializer serializer, ActorRef actorRef) {
            this.serializer = serializer;
            this.actorRef = actorRef;
        }

        @Override
        public Receive createReceive() {
            Receive receive = super.receiveBuilder()
                    .matchEquals("start", message -> {
                        for (int i = 0; i < 10; i++) {
                            PBCustomer.Customer customer = PBCustomer.Customer.newBuilder()
                                    .setName("Yang")
                                    .setTel("client tell")
                                    .setAge(i)
                                    .build();
                            byte[] bytes = serializer.toBinary(customer);
                            actorRef.tell(bytes, getSelf());

                            actorRef.tell(customer, self());
                        }
                    })
                    .match(byte[].class, message -> {
                        PBCustomer.Customer customer = (PBCustomer.Customer) serializer.fromBinary(message, PBCustomer.Customer.class);
                        System.out.println(customer);
                    }).build();
            return receive;
        }
    }


    public static void main(String[] args) throws Exception{

        final Config config = ConfigFactory.parseURL(TestAkkaSerial.class.getClassLoader().getResource("serial.conf"));

        ActorSystem system = ActorSystem.create("text_system", config);
        Serialization serialization = SerializationExtension.get(system);
        Serializer serializer = serialization.serializerFor(PBCustomer.Customer.class);

        ActorRef server = system.actorOf(Props.create(ServerActor.class, () -> new ServerActor(serializer)), "server");
        ActorRef client = system.actorOf(Props.create(ClientActor.class, () -> new ClientActor(serializer, server)));

        client.tell("start", client);
    }
}
