package com.study.akka.serializer;

import akka.actor.*;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.Option;

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
//                    selection.tell("printit", getSelf());

                    PBCustomer.Customer customer = PBCustomer.Customer.newBuilder()
                            .setName("Yang")
                            .setTel("client tell")
                            .setAge(2)
                            .build();
                    selection.tell(customer, getSelf());

                    TestWithSerialConfigByJava java = new TestWithSerialConfigByJava("name", 3);

                    TestWithSerialConfigByProto proto = new TestWithSerialConfigByProto("protoName", 34);

                    selection.tell(java, getSelf());
                    selection.tell(proto, getSelf());

                })
                .matchEquals("i print it success", p -> {
                    System.out.println("client get processor! --》 i print it success");
                    System.out.println("client get the sender is: " + sender());
                })
                .match(DeadLetter.class, message -> {
                    System.out.println("deadLetter");
                    System.out.println(message.message());
                })
                .matchAny(message -> {
                    System.out.println("matchAny");
                    System.out.println(message);
                })
                .build();
    }


    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception, Exception {
        reason.printStackTrace();
        super.preRestart(reason, message);
    }

    @Override
    public void postRestart(Throwable reason) throws Exception, Exception {
        reason.printStackTrace();
        super.postRestart(reason);
    }

    /**
     * 这个案例只是测试 akka 的序列化和反序列化的原理，并不完整。
     * @param args
     */
    public static void main(String[] args) {

        final Config config = ConfigFactory.parseURL(TestAkkaSerial.class.getClassLoader().getResource("application.conf"));

        // 启动client的时候需要去修改一下 application.conf 的 port ，让其和server的端口不一致
        // server 用的是 25520
        // client 用的是 25521
        ActorSystem system = ActorSystem.create("test-system-client", config);

        try {
            Serialization serialization = SerializationExtension.get(system);
            Serializer customer_serializer = serialization.serializerFor(PBCustomer.Customer.class);
            Serializer testWithSerialConfigByJava_serializer = serialization.serializerFor(TestWithSerialConfigByJava.class);
            Serializer testWithSerialConfigByProto_serializer = serialization.serializerFor(TestWithSerialConfigByProto.class);
            Serializer string_serializer = serialization.serializerFor(String.class);

            // Create top level supervisor
            ActorRef supervisor = system.actorOf(Client.props(), "test-supervisor-client");

            system.eventStream().subscribe(supervisor, DeadLetter.class);

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