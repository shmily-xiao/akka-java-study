package com.study.akka.routers;

import akka.actor.*;
import akka.routing.*;
import scala.collection.immutable.IndexedSeq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static akka.japi.Util.immutableIndexedSeq;

/**
 * 路由也可以被创建为一个独立的 Actor，管理路由器本身，并从配置中加载路由逻辑和其他设置。
 *
 * @author wzj
 * @date 2021/05/19
 */
public class Worker implements Serializable {

    private static final long serialVersionUID = -1472944500277538613L;

    public final String payload;

    public Worker(String payload){
        this.payload = payload;
    }
}

class Master extends AbstractActor {
    Router router;

    {
        List<Routee> routees = new ArrayList<Routee>();
        for (int i = 0; i < 5; i++){
            ActorRef actorRef = getContext().actorOf(Props.create(Worker.class));
            getContext().watch(actorRef);
            routees.add(new ActorRefRoutee(actorRef));
        }
        router =  new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Worker.class,
                        message -> {
                            router.route(message, getSender());
                        })
                .match(
                        Terminated.class,
                        message -> {
                            router = router.removeRoutee(message.actor());
                            ActorRef r = getContext().actorOf(Props.create(Worker.class));
                            getContext().watch(r);
                            router = router.addRoutee(new ActorRefRoutee(r));
                        }
                ).build();
    }

    /**
     * 等同这样的配置
     * akka.actor.deployment {
     *   /parent/router1 {
     *     router = round-robin-pool
     *     nr-of-instances = 5
     *   }
     * }
     */
    private void test(){
        ActorRef router2 = getContext().actorOf(new RoundRobinPool(5).props(Props.create(Worker.class)), "router2");

    }

    /**
     * 为了远程部署路由器，请将路由配置包装在RemoteRouterConfig中，并附加要部署到的节点的远程地址。
     *
     * 远程部署要求类路径中包含akka-remote模块。
     */
    private void remoteRouter(){
        Address[] addresses = {
                new Address("akka.tcp", "remotesys", "otherhost", 1234),
                AddressFromURIString.parse("akka.tcp://othersys@notherhost:1234")
        };

//        ActorRef routerRemote = getContext().actorOf(new RemoteRouterConfig(new RoundRobinPool(5), addresses).props(Props.create(Echo.class)));
    }
}

class RedundancyRoutingLogic implements RoutingLogic{
    private final int nbrCopies;

    public RedundancyRoutingLogic(int nbrCopies) {
        this.nbrCopies = nbrCopies;
    }
    // 循环路由
    RoundRobinRoutingLogic routingLogic = new RoundRobinRoutingLogic();

    @Override
    public Routee select(Object message, IndexedSeq<Routee> routees) {
        List<Routee> targets  = new ArrayList<>();
        for (int i=0; i < nbrCopies; i++){
            targets.add(routingLogic.select(message, routees));
        }
        return new SeveralRoutees(targets);
    }
}

class TestRoutee implements Routee{
    public final int n;

    public TestRoutee(int n){
        this.n = n;
    }

    @Override
    public void send(Object message, ActorRef sender) {
        System.out.println(n);
    }

    @Override
    public int hashCode() {
        return n;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof TestRoutee) && n == ((TestRoutee) obj).n;
    }
}
class Main{
    public static void main(String[] args) {
        RedundancyRoutingLogic logic = new RedundancyRoutingLogic(3);

        List<Routee> routeeList = new ArrayList<>();
        for (int n=1; n <= 7; n++){
            routeeList.add(new TestRoutee(n));
        }
        IndexedSeq<Routee> routees = immutableIndexedSeq(routeeList);
        SeveralRoutees r1 = (SeveralRoutees)logic.select("msg", routees);
        System.out.println(r1.getRoutees().get(0));
        System.out.println(r1.getRoutees().get(1));
        System.out.println(r1.getRoutees().get(2));
        System.out.println(routeeList.get(0));
        System.out.println(routeeList.get(1));
        System.out.println(routeeList.get(2));
        System.out.println(" ----------  ");


        SeveralRoutees r2 = (SeveralRoutees)logic.select("msg", routees);
        System.out.println(r2.getRoutees().get(0));
        System.out.println(r2.getRoutees().get(1));
        System.out.println(r2.getRoutees().get(2));
        System.out.println(routeeList.get(3));
        System.out.println(routeeList.get(4));
        System.out.println(routeeList.get(5));
        System.out.println(" ----------  ");


        SeveralRoutees r3 = (SeveralRoutees)logic.select("msg", routees);
        System.out.println(r3.getRoutees().get(0));
        System.out.println(r3.getRoutees().get(1));
        System.out.println(r3.getRoutees().get(2));
        System.out.println(routeeList.get(6));
        System.out.println(routeeList.get(0));
        System.out.println(routeeList.get(1));
        System.out.println(" ----------  ");

    }
}