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
        // 定义初始状态和初始数据
        startWith(FsmTest.State.Idle, FsmTest.Uninitialized.UNINITIALIZED);

        // 是要处理的每个状态的声明（可能是多个状态，传递的PartialFunction将使用orElse连接）
        when(
                FsmTest.State.Idle, // state
                // 我们从Idle状态开始，使用Uninitialized数据，其中只处理SetTarget()消息
                matchEvent(
                        FsmTest.SetTarget.class, // event
                        FsmTest.Uninitialized.class, // data
                        // stay准备结束此事件的处理，以避免离开当前状态，
                        // 而using修饰符使 FSM 用包含目标Actor的引用 的Todo() 对象替换内部状态（此时Uninitialized在这个点）
                        (setTarget, uninitialized) ->  // action
                                // using(nextStateData)
                                stay().using(new FsmTest.Todo(setTarget.getRef(), new LinkedList<>()))
                )
        );

        // 你可以声明多个这样的块，如果发生状态转换（即只有当状态实际更改时），所有这些块都将尝试匹配行为
        onTransition(
                //  (fromState, toState, apply)
                matchState(
                        FsmTest.State.Active,
                        FsmTest.State.Idle,
                        () -> {
                            // reuse this matcher
                            final UnitMatch<FsmTest.Data> m =
                                    UnitMatch.create(
                                            // 这里处理的第一个案例是将Queue() 请求添加到内部队列并进入Active状态
                                            // ，但前提是在接收到Queue()事件时，FSM 数据需要事先就初始化好了
                                            matchData(
                                                    FsmTest.Todo.class,
                                                    todo -> todo.getTarget().tell(new FsmTest.Batch(todo.getQueue()), getSelf())
                                            ));
                        }
                )
                 // (fromState, toState, apply)
                .state(
                        FsmTest.State.Idle,
                        FsmTest.State.Active,
                        () -> {
                            // Do something here
                            System.out.println("fsm out： from idle to active");
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

        // 因此我们利用以下事实：未由when()块处理的任何事件都传递给whenUnhandled()块
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

        // 最后使用initialize启动它，它执行到初始状态的转换并设置定时器
        initialize();
    }

}
