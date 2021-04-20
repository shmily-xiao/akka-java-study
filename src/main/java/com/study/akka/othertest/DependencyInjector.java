package com.study.akka.othertest;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.IndirectActorProducer;
import akka.actor.Props;

/**
 * 依赖注入
 * @author wzj
 * @date 2021/04/19
 */
public class DependencyInjector implements IndirectActorProducer {

    private final Object applicationContext;

    private final String beanName;

    public DependencyInjector(Object applicationContext, String beanName) {
        this.applicationContext = applicationContext;
        this.beanName = beanName;
    }

    @Override
    public Actor produce() {
        return null;
    }

    @Override
    public Class<? extends Actor> actorClass() {
        return null;
    }
//    @Override
//    public Actor produce() {
//        TheActor result;
//        result = new TheActor( (String) applicationContext);
//        return result;
//    }
//
//    @Override
//    public Class<? extends Actor> actorClass() {
//        return TheActor.class;
//    }
//
//    final ActorRef myActor = getContext().actorOf(Props.create(DependencyInjector.class, applicationContext, "TheActor"), "TheActor");
}

