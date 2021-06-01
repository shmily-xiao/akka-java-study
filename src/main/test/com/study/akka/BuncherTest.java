package com.study.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.study.akka.fsm.Buncher;
import com.study.akka.fsm.FsmTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;

import static com.study.akka.fsm.FsmTest.Flush.FLUSH;

/**
 * @author wzj
 * @date 2021/05/27
 */
public class BuncherTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup(){
        // 创建了一个主actor
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown(){
        TestKit.shutdownActorSystem(system);
        system=null;
    }

    @Test
    public void  testBuncherActorBatchesCorrectly(){
        new TestKit(system){
            {
                final ActorRef buncher = system.actorOf(Props.create(Buncher.class));
                final ActorRef probe = getRef();

                buncher.tell(new FsmTest.SetTarget(probe), probe);

                buncher.tell(new FsmTest.Queue(42), probe);
                buncher.tell(new FsmTest.Queue(43), probe);
                LinkedList<Object> list1  = new LinkedList<>();
                list1.add(42);
                list1.add(43);
                expectMsgEquals(new FsmTest.Batch(list1));
                System.out.println(" -------------------- ");

                buncher.tell(new FsmTest.Queue(44), probe);
                buncher.tell(FLUSH, probe);
                buncher.tell(new FsmTest.Queue(45), probe); // flush之后没有出现这条记录
                LinkedList<Object> list2  = new LinkedList<>();
                list2.add(44);
                expectMsgEquals(new FsmTest.Batch(list2));

                System.out.println(" -------------------- ");

                LinkedList<Object> list3 = new LinkedList<>();
                list3.add(45);
                expectMsgEquals(new FsmTest.Batch(list3));

                system.stop(buncher);
            }
        };
    }


    @Test
    public void testBuncherActorDoesntBatchUninitialized(){
        new TestKit(system){
            {
                final ActorRef buncher = system.actorOf(Props.create(Buncher.class));
                final ActorRef probe = getRef();

                buncher.tell(new FsmTest.Queue(42), probe);
                expectNoMessage();

                system.stop(buncher);
            }
        };
    }
}
