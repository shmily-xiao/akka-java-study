

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
## 调度
## 路由
——
