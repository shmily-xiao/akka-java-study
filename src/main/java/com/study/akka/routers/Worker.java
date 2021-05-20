package com.study.akka.routers;

import akka.actor.*;
import akka.routing.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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