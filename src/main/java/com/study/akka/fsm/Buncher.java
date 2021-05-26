package com.study.akka.fsm;

import akka.actor.AbstractFSM;
import akka.japi.pf.UnitMatch;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * State(S) x Event(E) -> Actions (A), State(S’)
 * 如果我们处于状态S，并且事件E发生，那么我们应该执行操作A，并向状态S’过渡。
 *
 * @author wzj
 * @date 2021/05/26
 */
public class Buncher extends AbstractFSM<FsmTest.State, FsmTest.Data> {
    {
        startWith(FsmTest.State.Idle, FsmTest.Uninitialized.UNINITIALIZED);

        when(
                FsmTest.State.Idle,
                matchEvent(
                        FsmTest.SetTarget.class,
                        FsmTest.Uninitialized.class,
                        (setTarget, uninitialized) ->
                                stay().using(new FsmTest.Todo(setTarget.getRef(), new LinkedList<>()))
                )
        );

        onTransition(
                matchState(
                        FsmTest.State.Active,
                        FsmTest.State.Idle,
                        () -> {
                            // reuse this matcher
                            final UnitMatch<FsmTest.Data> m =
                                    UnitMatch.create(
                                            matchData(
                                                    FsmTest.Todo.class,
                                                    todo -> todo.getTarget().tell(new FsmTest.Batch(todo.getQueue()), getSelf())
                                            ));
                        }
                )
                .state(
                        FsmTest.State.Idle,
                        FsmTest.State.Active,
                        () -> {
                            // Do something here
                            System.out.println("fsm out");
                        }
                )
        );

        when(
                FsmTest.State.Active,
                Duration.ofSeconds(1L),
                matchEvent(
                        Arrays.asList(FsmTest.Flush.class, StateTimeout()),
                        FsmTest.Todo.class,
                        (event, todo) -> goTo(FsmTest.State.Idle).using(todo.copy(new LinkedList<>()))
                )
        );

        whenUnhandled(
                // eventType, dataType, do
                matchEvent(
                        FsmTest.Queue.class,
                        FsmTest.Todo.class,
                        (queue, todo) -> goTo(FsmTest.State.Active).using(todo.addElement(queue.getObj()))
                )
                .anyEvent(
                        (event, state) -> {
                            log().warning("received unhandled request {} in state {}/{}", event, stateName(), state);
                            return stay();
                        }
                )
        );

        initialize();
    }

}
