package com.study.akka;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import com.study.akka.iot.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.management.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Test
    public void testRegisterDeviceActor(){
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(DeviceGroup.props("group"));

        actorRef.tell(new Device.RequestTrackDevice("group", "device1"), testKit.getRef());
        testKit.expectMsgClass(Device.DeviceRegistered.class);
        ActorRef deviceActor1 = testKit.getLastSender();

        actorRef.tell(new Device.RequestTrackDevice("group", "device2"), testKit.getRef());
        testKit.expectMsgClass(Device.DeviceRegistered.class);
        ActorRef deviceActor2 = testKit.getLastSender();
        Assert.assertNotEquals(deviceActor1, deviceActor2);

        deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), testKit.getRef());
        Assert.assertEquals(0L, testKit.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
        deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), testKit.getRef());
        Assert.assertEquals(1L, testKit.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
    }

    @Test
    public void testIgnoreRequestsWrongGroupId(){
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(DeviceGroup.props("group"));

        actorRef.tell(new Device.RequestTrackDevice("wrongGroup", "device1"), testKit.getRef());
//        actorRef.tell(new Device.RequestTrackDevice("group", "device1"), testKit.getRef());
        testKit.expectNoMessage();
    }

    /**
     * 我们测试在添加了一些设备之后，是否能返回正确的 ID 列表
     */
    @Test
    public void testListActiveDevices(){
        TestKit probe = new TestKit(system);

        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
        // 消息的作用域是什么？
        groupActor.tell(new Device.RequestTrackDevice("group", "device1"),probe.getRef());
        probe.expectMsgClass(Device.DeviceRegistered.class);

        groupActor.tell(new Device.RequestTrackDevice("group", "device2"),probe.getRef());
        probe.expectMsgClass(Device.DeviceRegistered.class);

        groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.getRef());
        DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
        Assert.assertEquals(0L, reply.requestId);
        Assert.assertEquals(Stream.of("device1","device2").collect(Collectors.toSet()),reply.ids);
    }

    /**
     * 测试用例确保在设备 Actor 停止后正确删除设备 ID
     */
    @Test
    public void testListActiveDevicesAfterOneShutdown(){
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new Device.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(Device.DeviceRegistered.class);
        ActorRef toShutdown = probe.getLastSender();

        groupActor.tell(new Device.RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(Device.DeviceRegistered.class);

        groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.getRef());
        DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
        Assert.assertEquals(0L, reply.requestId);
        Assert.assertEquals(Stream.of("device1","device2").collect(Collectors.toSet()), reply.ids);

        System.out.println("------------------///-----------------" + reply.requestId);

        // 观察 device1 的actor
        probe.watch(toShutdown);
        toShutdown.tell(PoisonPill.getInstance(), ActorRef.noSender());
        probe.expectTerminated(java.time.Duration.ofSeconds(1),toShutdown);

        probe.awaitAssert(java.time.Duration.ofSeconds(3),() -> {
            groupActor.tell(new DeviceGroup.RequestDeviceList(1L), probe.getRef());
            DeviceGroup.ReplyDeviceList r = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
            System.out.println("-----------------++----------------- " + r.requestId);
            Assert.assertEquals(1L, r.requestId);
            Assert.assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
            return null;
        });
    }


    /**
     *  未测试通过
     */
    @Test
    public void testDeviceManager(){
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceManager.props());

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(Device.DeviceRegistered.class);

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), groupActor);
        probe.expectMsgClass(DeviceManager.DeviceRegisted.class);
    }

    /**
     *  我们在有两个设备的情况下进行测试，两个设备都报告了温度
     */
    @Test
    public void testReturnTemperatureValueForWorkingDevices(){
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).getRequestId());

        // 报告了温度
        queryActor.tell(new ResponseTemperature(0L, Optional.of(1.0)), device1.getRef());
        queryActor.tell(new ResponseTemperature(0L, Optional.of(2.0)), device2.getRef());

        DeviceGroup.RespondAllTemperatures response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(1L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

        Assert.assertEquals(expectedTemperatures, response.getTemperatures());
    }


    /**
     * 这是一个很好的例子，但我们知道有时设备不能提供温度测量。
     * 有一个使用设备没有发送温度
     */
    @Test
    public void testReturnTemperatureNoteAvailableForDeviceWithNoReadings(){
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).getRequestId());

        queryActor.tell(new ResponseTemperature(0L, Optional.empty()), device1.getRef());
        queryActor.tell(new ResponseTemperature(0L, Optional.of(2.0)), device2.getRef());

        DeviceGroup.RespondAllTemperatures response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(1L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", DeviceGroup.TemperatureNotAvailable.INSTANCE);
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

        Assert.assertEquals(expectedTemperatures, response.getTemperatures());
    }


    /**
     * 设备2 在响应之前中毒了，关闭了
     */
    @Test
    public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering(){
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).getRequestId());

        queryActor.tell(new ResponseTemperature(0L, Optional.of(1.0)), device1.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroup.RespondAllTemperatures  response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(1L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperatures.put("device2", DeviceGroup.DeviceNotAvailable.INSTANCE);

        Assert.assertEquals(expectedTemperatures, response.getTemperatures());

    }

    /**
     * 如果你还记得，还有一个用例与设备 Actor 停止相关。
     * 我们可以从一个设备 Actor 得到一个正常的回复，
     * 但是随后接收到同一个 Actor 的一个Terminated消息。
     * 在这种情况下，我们希望保持第一次回复，而不是将设备标记为DeviceNotAvailable
     */
    @Test
    public void testReturnTemperatureReadingEventIfDeviceStopsAfterAnswering(){
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(3, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).getRequestId());

        queryActor.tell(new ResponseTemperature(0L, Optional.of(1.0)), device1.getRef());
        queryActor.tell(new ResponseTemperature(0L, Optional.of(2.0)), device2.getRef());
        // 报告完了数据之后立马失效
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        DeviceGroup.RespondAllTemperatures response = requester.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(1L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

        Assert.assertEquals(expectedTemperatures, response.getTemperatures());

    }

    /**
     * 最后一种情况是，并非所有设备都能及时响应。
     */
    @Test
    public void testReturnDeviceTimedOutIfDoesNotAnswerInTime(){
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId =  new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(actorToDeviceId, 1L, requester.getRef(), new FiniteDuration(1, TimeUnit.SECONDS)));

        Assert.assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).getRequestId());
        Assert.assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).getRequestId());

        queryActor.tell(new ResponseTemperature(0L, Optional.of(1.0)), device1.getRef());

        DeviceGroup.RespondAllTemperatures response = requester.expectMsgClass(java.time.Duration.ofSeconds(5), DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(1L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperatures.put("device2", DeviceGroup.DeviceTimeOut.INSTANCE);

        Assert.assertEquals(expectedTemperatures, response.getTemperatures());
    }

    /**
     *
     */
    @Test
    public void testCollectTemperaturesFromAllActiveDevices(){
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegisted.class);
        ActorRef deviceActor1 = probe.getLastSender();

        groupActor.tell(new DeviceManager.RequestTrackDevice("group","device2"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegisted.class);
        ActorRef deviceActor2 = probe.getLastSender();

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device3"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegisted.class);
        ActorRef deviceActor3 = probe.getLastSender();

        // deviceActor1、deviceActor2、 deviceActor3 都是子actor
        deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), probe.getRef());
        Assert.assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
        deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), probe.getRef());
        Assert.assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

        groupActor.tell(new DeviceGroup.RequestAllTemperatures(0L), probe.getRef());
        DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        Assert.assertEquals(0L, response.getRequestId());

        Map<String, DeviceGroup.TemperatureReading> expectedTemperature = new HashMap<>();
        expectedTemperature.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperature.put("device2", new DeviceGroup.Temperature(2.0));
        expectedTemperature.put("device3", DeviceGroup.TemperatureNotAvailable.INSTANCE);

        Assert.assertEquals(expectedTemperature, response.getTemperatures());

    }
}
