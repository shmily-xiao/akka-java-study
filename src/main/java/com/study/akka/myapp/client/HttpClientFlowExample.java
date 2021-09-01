package com.study.akka.myapp.client;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

/**
 * @author wzj
 * @date 2021/08/31
 */
public class HttpClientFlowExample {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow=
                Http.get(system).connectionTo("localhost:25520").http();

        // This is actually a bad idea in general. Even if the `connectionFlow` was instantiated only once above,
        // a new connection is opened every single time, `runWith` is called. Materialization (the `runWith` call)
        // and opening up a new connection is slow.
        //
        // The `outgoingConnection` API is very low-level. Use it only if you already have a `Source[HttpRequest, _]`
        // (other than Source.single) available that you want to use to run requests on a single persistent HTTP
        // connection.
        //
        // Unfortunately, this case is so uncommon, that we couldn't come up with a good example.
        //
        // In almost all cases it is better to use the `Http().singleRequest()` API instead.
        /**
         * 总的来说，这其实是个坏主意。即使“connectionFlow”在上面只实例化了一次，每次都会打开一个新的连接，调用“runWith”。
         * 序列化(“runWith”调用)和打开一个新连接是缓慢的。
         *
         * “outgoingConnection”API是非常低级的。只有当你已经有一个' Source[HttpRequest， _] '(除了Source.single)可用，你想用它在一个持久的HTTP连接上运行请求时，才可以使用它。
         *
         * 不幸的是，这种情况很少见，我们想不出一个好的例子。
         * 在几乎所有情况下，使用' Http(). singlerequest () ' API会更好。
         */
        final CompletionStage<HttpResponse> responseFuture = Source.single(HttpRequest.create("/"))
                .via(connectionFlow)
                .runWith(Sink.head(), system);

    }
}
