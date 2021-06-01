package com.study.akka.fsm;

import akka.actor.AbstractFSM;
import akka.actor.AbstractLoggingFSM;
import akka.japi.pf.UnitMatch;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * State(S) x Event(E) -> Actions (A), State(S’)
 * 如果我们处于状态S，并且事件E发生，那么我们应该执行操作A，并向状态S’过渡。
 *
 * 可以使用goto(S)或stay()实现相同的状态转换（当前处于状态S时）。
 * 不同之处在于，goto(S)会发出一个事件S->S，该事件可以由onTransition处理，而stay()则不会
 * @author wzj
 * @date 2021/05/26
 */
public class Buncher extends AbstractLoggingFSM<FsmTest.State, FsmTest.Data> {
    @Override
    public int logDepth() {
        return 12;
    }

    {
        // 起点
        // 定义初始状态和初始数据
        // startWith(state, data[, timeout])
        startWith(FsmTest.State.Idle, FsmTest.Uninitialized.UNINITIALIZED);

        // 是要处理的每个状态的声明（可能是多个状态，传递的PartialFunction将使用orElse连接）
        // when(<name>[, stateTimeout = <timeout>])(stateFunction)
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
        // whenUnhandled(stateFunction)
        whenUnhandled(
                // eventType, dataType, do
                // 这里处理的第一个案例是将Queue() 请求添加到内部队列并进入Active状态
                // ，但前提是在接收到Queue()事件时，FSM 数据需要事先就初始化好了
                matchEvent(
                        FsmTest.Queue.class,
                        FsmTest.Todo.class,
                        (queue, todo) -> goTo(FsmTest.State.Active).using(todo.addElement(queue.getObj()))
                )
                .anyEvent(
                        (event, state) -> {
                            log().warning("received unhandled request {} in state {}/{}",
                                    event,
                                    stateName(),
                                    state);
                            return stay();
                        }
                )
        );

        // 内部监控
        // 唯一缺少的部分是Batches实际发送到目标的位置，为此我们使用了onTransition机制：
        // 你可以声明多个这样的块，如果发生状态转换（即只有当状态实际更改时），所有这些块都将尝试匹配行为
        //
        // 过渡期
        // onTransition(handler)
        onTransition(
                //  (fromState, toState, apply)
                matchState(
                        FsmTest.State.Active,
                        FsmTest.State.Idle,
                        () -> {
                            // reuse this matcher
                            final UnitMatch<FsmTest.Data> m =
                                    UnitMatch.create(
                                            matchData(
                                                    FsmTest.Todo.class,
                                                    // 从队列中取对象（也叫做消息）出来，发送出去
                                                    todo -> todo.getTarget().tell(new FsmTest.Batch(todo.getQueue()), getSelf())
                                            ));
                            m.match(stateData());
                            System.out.println(stateData());
                            System.out.println(nextStateData());
                            System.out.println("fsm matchState out: from active to idle");
                        }
                )
                        // 在状态更改期间，旧的状态数据通过stateData()可用，
                        // 如展示的这样，新的状态数据将作为nextStateData()可用
                        // (fromState, toState, apply)
                        .state(
                                FsmTest.State.Idle,
                                FsmTest.State.Active,
                                () -> {
                                    System.out.println(stateData());
                                    System.out.println(nextStateData());
                                    // Do something here
                                    System.out.println("fsm state out： from idle to active");
                                }
                        )
        );

        onTermination(
                matchStop(
                        Normal(),
                        (state, data) -> {
                            /* Do something here */
                            System.out.println("Normal stop");
                        })
                        .stop(
                                Shutdown(),
                                (state, data) -> {
                                    /* Do something here */
                                    System.out.println("Shutdown stop");
                                })
                        .stop(
                                Failure.class,
                                (reason, state, data) -> {
                                    /* Do something here */
                                    System.out.println("Failure stop");

                                    String lastEvents = getLog().mkString("\n\t");
                                    log().warning("Failure in state "
                                            + state
                                            + " with data "
                                            + data
                                            + "\n"
                                            + "Events leading up to this point:\n\t"
                                            + lastEvents);
                                }));

        // 最后使用initialize启动它，它执行到初始状态的转换并设置定时器
        initialize();
    }

}
