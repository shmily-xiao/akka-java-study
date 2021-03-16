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

    //@Test
    //public void testReplyToRegistrationRequests() {
    //  TestKit probe = new TestKit(system);
    //  ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
    //
    //  deviceActor.tell(new DeviceManager.RequestTrackDevice("group", "device"), probe.getRef());
    //  probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
    //  assertEquals(deviceActor, probe.getLastSender());
    //}
    //
    //@Test
    //public void testIgnoreWrongRegistrationRequests() {
    //  TestKit probe = new TestKit(system);
    //  ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
    //
    //  deviceActor.tell(new DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.getRef());
    //  probe.expectNoMessage();
    //
    //  deviceActor.tell(new DeviceManager.RequestTrackDevice("group", "wrongDevice"), probe.getRef());
    //  probe.expectNoMessage();
    //}

    /**
     * 一个成功注册
     */
    @Test
    public void testReplayToRegistrationRequests(){
        TestKit testKit = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "deviceId"));

        deviceActor.tell(new Device.RequestTrackDevice("group", "deviceId"), testKit.getRef());
        testKit.expectMsgClass(Device.DeviceRegistered.class);
        Assert.assertEquals(deviceActor, testKit.getLastSender());

    }

    /**
     * group 和 device 不匹配，注册不成功 。因为在device 的createReceive()里面else 分支没有 tell 返回值
     */
    @Test
    public void testIgnoreWrongRegistrationRequests(){
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(Device.props("group", "device"));

        actorRef.tell(new Device.RequestTrackDevice("wringGroup", "device"), testKit.getRef());
        testKit.expectNoMessage();

        actorRef.tell(new Device.RequestTrackDevice("group", "wrongDevice"), testKit.getRef());
        testKit.expectNoMessage();
    }


}
