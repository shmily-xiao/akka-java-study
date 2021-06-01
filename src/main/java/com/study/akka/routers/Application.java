package com.study.akka.routers;

import akka.routing.Routee;
import akka.routing.SeveralRoutees;
import scala.collection.immutable.IndexedSeq;

import java.util.ArrayList;
import java.util.List;

import static akka.japi.Util.immutableIndexedSeq;

/**
 * @author wzj
 * @date 2021/06/01
 */
public class Application {
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
