package com.study.akka.fsm;

import akka.actor.ActorRef;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author wzj
 * @date 2021/05/26
 */
public class FsmTest {
    /**
     * 启动它需要SetTarget，为要传递的Batches设置目标；Queue将添加到内部队列，
     * 而Flush将标记突发（burst）的结束。
     */
    static final class SetTarget{
        private final ActorRef ref;

        public SetTarget(ActorRef ref) {
            this.ref = ref;
        }

        public ActorRef getRef() {
            return ref;
        }

        @Override
        public String toString() {
            return "SetTarget{" + "ref=" + ref + '}';
        }
    }

    static final class Queue {
        private final Object obj;

        public Queue(Object obj){
            this.obj = obj;
        }

        public Object getObj() {
            return obj;
        }

        @Override
        public String toString() {
            return "Queue{" +
                    "obj=" + obj +
                    '}';
        }
    }


    static final class Batch{
        private final List<Object> list;

        public Batch(List<Object> list) {
            this.list = list;
        }

        public List<Object> getList() {
            return list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Batch batch = (Batch) o;
            return Objects.equals(list, batch.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Batch{list=");
            list.stream().forEachOrdered(
                    o -> {
                        builder.append(o);
                        builder.append(",");
                    });
            int len = builder.length();
            builder.replace(len, len, "}");
            return builder.toString();
        }
    }
    static enum Flush{
        FLUSH
    }

    // states
    enum State{
        Idle,
        Active
    }

    // state data
    interface Data{}


    enum Uninitialized implements Data {
        UNINITIALIZED
    }

    static final class Todo implements Data {
        private final ActorRef target;
        private final List<Object> queue;

        public Todo(ActorRef target, List<Object> queue) {
            this.target = target;
            this.queue = queue;
        }

        public ActorRef getTarget() {
            return target;
        }

        public List<Object> getQueue() {
            return queue;
        }

        public Todo addElement(Object element){
            List<Object> nQueue = new LinkedList<>(queue);
            nQueue.add(element);
            return new Todo(this.target, nQueue);
        }

        public Todo copy(List<Object> queue){
            return new Todo(this.target, queue);
        }

        public Todo copy(ActorRef target){
            return new Todo(target, this.queue);
        }
    }
}
