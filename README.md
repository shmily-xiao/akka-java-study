

# akka-java-study
study akka with java

first with this pages!
https://cloud.tencent.com/developer/article/1438044


### CPI :

指令平均周期数（英语：Cycle Per Instruction, CPI），也称每指令周期，即执行在计算机体系结构中一条指令所需要的平均时钟周期（机器主频的倒数）数.
一个完整的计算机系统执行时间，即指令周期指从内存中取出并执行该条指令所用的全部时间，它取决于硬件结构和各部件的性能.


# Actor 的体系结构
  https://github.com/guobinhit/akka-guide/blob/master/articles/getting-started-guide/tutorial_1.md
 
 

# 第4章节
- 向设备 Actor 添加注册支持
- 跟踪组内的设备 Actor
- 管理设备actor

# 第 5 部分: 查询设备组
- 实现查询功能
- done

#
# 什么是actor done

# 监督和监控 done
- 有一些没有十分理解
-
# Actor 引用、路径和地址 done

# Actors begin
## 依赖注入
## [akka java in spring](https://github.com/typesafehub/activator-akka-java-spring)
- actor 依赖注入 完结，但是这个章节的代码没有办法跑

## Actor 的生命周期
- 生命周期监控，或称为 DeathWatch

## 发送消息

## 接收消息
- 如果对某些 Actor 来说，验证ReceiveBuilder匹配逻辑是一个瓶颈，那么你可以考虑通过扩展UntypedAbstractActor而不是AbstractActor来在较低的级别实现它。ReceiveBuilder创建的分部函数由每个match语句的多个lambda表达式组成，其中每个lambda都引用要运行的代码。这是 JVM 在优化时可能会遇到的问题，并且产生的代码的性能可能不如非类型化版本。当扩展UntypedAbstractActor时，每个消息都作为非类型化Object接收，你必须以其他方式检查并转换为实际的消息类型
## 停止 Actor

## 协调关闭
## become
## unbecome

## 容错
## 调度。-> 线程池
## 邮箱 --> 队列
## 路由。--> 分发
### RoundRobinPool 和 RoundRobinGroup
循环

### RandomPool 和 RandomGroup
此路由类型为每条消息随机选择的一个路由器类型。

### BalancingPool
一种将工作从繁忙的路由器重新分配到空闲的路由器的路由。所有路由共享同一个邮箱。

注释 1：BalancingPool的特性是，它的路由器没有真正不同的身份，它们有不同的名称，但在大多数情况下，与它们交互不会以正常的 Actor 结束。因此，你不能将其用于需要状态保留在路由中的工作流，在这种情况下，你必须在消息中包含整个状态。使用「SmallestMailboxPool」，你可以拥有一个垂直扩展的服务，该服务可以在回复原始客户端之前以状态方式与后端的其他服务交互。另一个优点是，它不像BalancingPool那样对消息队列的实现进行限制。

注释 2：在路由使用「BalancingPool」时，不要使用「Broadcast 消息」，详情见「特殊处理的消息」中的描述。


### SmallestMailboxPool
它是一个试图发送到邮箱中邮件最少的非挂起子路由器的路由。选择顺序如下：

选择邮箱为空的任何空闲路由（不处理邮件）
选择任何邮箱为空的路由
选择邮箱中挂起邮件最少的路由
选择任何远程路由器，远程 Actor 被认为是最低优先级，因为它们的邮箱大小是未知的

### BroadcastPool 和 BroadcastGroup
广播路由（broadcast router）把它接收到的信息转发给它的所有路由器。

### ScatterGatherFirstCompletedPool 和 ScatterGatherFirstCompletedGroup
ScatterGatherFirstCompletedRouter将消息发送到其所有路由器，然后等待它得到的第一个回复。此结果将被发送回原始发件人，而其他答复将被丢弃。
它期望在配置的持续时间内至少有一个回复，否则它将以akka.actor.Status.Failure中的akka.pattern.AskTimeoutException进行回复。

### TailChoppingPool 和 TailChoppingGroup
TailChoppingRouter首先将消息发送到一个随机选择的路由器，然后在一个小延迟后发送到第二个路由器（从剩余的路由器随机选择）等。它等待第一个回复，然后返回并转发给原始发送者，而其他答复将被丢弃。
此路由器的目标是通过对多个路由器执行冗余查询来减少延迟，前提是其他 Actor 之一的响应速度可能仍然比初始 Actor 快。
彼得·贝利斯在一篇博文中很好地描述了这种优化：「通过多余的工作来加速分布式查询」。

### ConsistentHashingPool 和 ConsistentHashingGroup
ConsistentHashingPool使用「一致性哈希」根据发送的消息选择路由。「这篇文章」详细描述了如何实现一致性哈希。

有 3 种方法定义一致性哈希键要使用的数据。

你可以使用路由的withHashMapper定义将传入消息映射到其一致的哈希键。这使发送方的决策透明。
消息可以实现akka.routing.ConsistentHashingRouter.ConsistentHashable。键是消息的一部分，它便于与消息一起定义。
消息可以包装到akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope，以定义用于一致性哈希键的数据。发送者知道要使用的键。
这些定义一致性哈希键的方法可以同时用于一个路由。首先尝试使用withHashMapper。

## 特殊处理的消息

### Broadcast 消息
大多数发送给路由 Actor 的消息将根据路由的路由逻辑进行转发。但是，有几种类型的消息具有特殊的行为。
请注意，这些特殊消息（Broadcast消息除外）仅由独立的路由 Actor 处理，而不是由「一个简单的路由」中描述的akka.routing.Router组件处理。

### PoisonPill 消息
PoisonPill消息对所有 Actor 都有特殊的处理，包括路由。当任何 Actor 收到PoisonPill消息时，该 Actor 将被停止


### Kill 消息
Kill消息是另一种具有特殊处理功能的消息类型。有关 Actor 如何处理Kill消息的一般信息，请参阅「」文档。

当Kill消息发送到路由时，路由会在内部处理消息，而不会将其发送到路由器。路由将抛出ActorKilledException并失败。然后，它将被恢复、重新启动或终止，这取决于它是如何被监督的。

作为路由器子代的路由器也将被挂起，并受应用于路由的监督指令的影响。不是路由器子代的路由器，即那些在路由外部创建的路由器，将不会受到影响。


### 管理消息
将akka.routing.GetRoutees发送到路由 Actor 将使其在akka.routing.Routees消息中返回当前使用的路由器。
将akka.routing.AddRoutee发送到路由 Actor 将把该路由器添加到其路由器集合中。
将akka.routing.RemoveRoutee发送到路由 Actor 将其路由器集合中删除该路由器。
将akka.routing.AdjustPoolSize发送到池路由 Actor 将向其路由器集合中添加或删除该数量的路由器。
这些管理消息可能在其他消息之后处理，因此，如果你立即发送AddRoutee，然后再发送普通消息，则不能保证在路由普通消息时已更改了路由器。如果你需要知道何时应用了更改，你可以发送AddRoutee，然后发送GetRoutees，当你收到Routees回复时，你将知道前面的更改已经应用。

## 可动态调整大小的池
所有池都可以与固定数量的路由器一起使用，或者使用调整大小策略动态调整路由器的数量。
有两种类型的大小调整器：默认Resizer和OptimalSizeExploringResizer。

### 默认 Resizer
默认的大小调整器（resizer）根据压力向上和向下调整池大小，由池中繁忙路由器的百分比度量。如果压力高于某个阈值，则会增大池的大小；如果压力低于某个阈值，则会减小池的大小。两个阈值都是可配置的。



# FSM 

State(S) x Event(E) -> Actions (A), State(S’)
如果我们处于状态S，并且事件E发生，那么我们应该执行操作A，并向状态S’过渡。


# 持久化

~
