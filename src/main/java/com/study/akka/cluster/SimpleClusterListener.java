package com.study.akka.cluster;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorLogging;

/**
 * @author wzj
 * @date 2021/12/07
 */
public class SimpleClusterListener extends AbstractActor {

    @Override
    public void preStart() throws Exception, Exception {

        super.preStart();
    }

    @Override
    public void postStop() throws Exception, Exception {
        super.postStop();
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
