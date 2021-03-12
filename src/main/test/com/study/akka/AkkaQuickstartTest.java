package com.study.akka;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.study.akka.iot.Device;
import com.study.akka.iot.ReadTemperature;
import com.study.akka.iot.ResponseTemperature;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.util.Optional;

public class AkkaQuickstartTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup(){
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown(){
        TestKit.shutdownActorSystem(system);
        system=null;
    }

    @Test
    public void testGreeterActorSendingOfGreeting(){
        final TestKit testProbe = new TestKit(system);
        final ActorRef helloGreeter = system.actorOf(Greeter.props("hello", testProbe.getRef()));
        helloGreeter.tell(new Greeter.WhoToGreet("Akka"), ActorRef.noSender());
        helloGreeter.tell(new Greeter.Greet(), ActorRef.noSender());
        Printer.Greeting  greeting = testProbe.expectMsgClass(Printer.Greeting.class);
        Assert.assertEquals("hello, Akka", greeting.message);

    }

    @Test
    public void testReplyWithEmptyReadingIIfNoTemperatureIsKnown(){
        TestKit probe = new TestKit(system);

        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
        deviceActor.tell(new ReadTemperature(42L), probe.getRef());

        ResponseTemperature response = probe.expectMsgClass(ResponseTemperature.class);

        Assert.assertEquals(42L, response.getRequestId());
        Assert.assertEquals(Optional.empty(), response.getValue());
    }

    @Test
    public void testReplyWithLatestTemperatureReading(){
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        // 发数据 消息
        deviceActor.tell(new Device.RecordTemperature(1L, 24.0), probe.getRef());
        Assert.assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

        // 读数据 消息
        deviceActor.tell(new ReadTemperature(2L), probe.getRef());
        ResponseTemperature response1 = probe.expectMsgClass(ResponseTemperature.class);

        Assert.assertEquals(2L, response1.getRequestId());
        Assert.assertEquals(Optional.of(24.0), response1.getValue());

        // 发数据 消息
        deviceActor.tell(new Device.RecordTemperature(3L, 55.0), probe.getRef());
        Assert.assertEquals(3L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

        // 读数据 消息
        deviceActor.tell(new ReadTemperature(4L), probe.getRef());
        ResponseTemperature response2 = probe.expectMsgClass(ResponseTemperature.class);
        Assert.assertEquals(4L, response2.getRequestId());
        Assert.assertEquals(Optional.of(55.0), response2.getValue());
    }



}
