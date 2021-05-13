package com.study.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;


/**
 * @author wzj
 * @date 2021/05/13
 */
public class FaultHandlingTest{
    static ActorSystem system;
    static Duration timeout = Duration.create(5, TimeUnit.SECONDS);

    @BeforeClass
    public static void start(){
        system = ActorSystem.create("FaultHandlingTest");
    }

    @AfterClass
    public static void cleanup(){
        TestKit.shutdownActorSystem(system, timeout);
        system = null;
    }

    @Test
    public void mustEmploySupervisorStrategy() throws Exception{
        Props superProps = Props.create(Supervisor.class);
        ActorRef supervisor = system.actorOf(superProps, "supervisor");
        ActorRef child = (ActorRef) Await.result(ask(supervisor, Props.create(Child.class), 5000), timeout);

        child.tell(42, ActorRef.noSender());
        assert Await.result(ask(child, "get", 5000), timeout).equals(42);

        child.tell(new ArithmeticException(), ActorRef.noSender());
        assert Await.result(ask(child, "get", 5000), timeout).equals(42);

        child.tell(new NullPointerException(), ActorRef.noSender());
        assert Await.result(ask(child, "get", 5000), timeout).equals(0);

        final TestProbe probe = new TestProbe(system);
        probe.watch(child);
        child.tell(new IllegalAccessException(), ActorRef.noSender());
        probe.expectMsgClass(Terminated.class);

    }

    @Test
    public void mustEmploySupervisorStrategy2() throws Exception{
        Props props = Props.create(Supervisor2.class);
        ActorRef supervisor = system.actorOf(props);
        ActorRef child = (ActorRef) Await.result(ask(supervisor, Props.create(Child.class), 5000), timeout);
        child.tell(23, ActorRef.noSender());
        assert Await.result(ask(child, "get", 5000), timeout).equals(23);

        child.tell(new Exception(), ActorRef.noSender());
        assert Await.result(ask(child, "get", 5000), timeout).equals(0);
    }

}
