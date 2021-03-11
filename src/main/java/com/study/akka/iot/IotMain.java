package com.study.akka.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * @author wzj
 * @date 2021/03/10
 */
public class IotMain {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("iot-system");

        try{
            ActorRef actorRef = system.actorOf(IotSupervisor.props(), "iot-supervisor");


            System.out.println("Press Enter to exit system");
            System.in.read();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            system.terminate();
        }
    }
}
