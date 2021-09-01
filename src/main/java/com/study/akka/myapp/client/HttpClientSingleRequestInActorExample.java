package com.study.akka.myapp.client;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import com.study.akka.Greeter;
import com.study.akka.Printer;
import scala.concurrent.ExecutionContextExecutor;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.pipe;

/**
 * 使用actor进行一个简单的请求
 * @author wzj
 * @date 2021/08/31
 */
public class HttpClientSingleRequestInActorExample extends AbstractActor {

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();

    static public Props props() {
        return Props.create(HttpClientSingleRequestInActorExample.class, () -> new HttpClientSingleRequestInActorExample());
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, url -> pipe(fetch(url), dispatcher).to(self()))
                .build();
    }

    CompletionStage<HttpResponse> fetch(String url) {
        System.out.println(url);
        return http.singleRequest(HttpRequest.create(url));
    }

    // 这样写不太对
    Flow<Pair<HttpRequest, Object>, String, NotUsed> fetchFlow(String url) {
        System.out.println(url);
        Flow<Pair<HttpRequest, Object>, String, NotUsed> ask = http.superPool().ask(context().self(), String.class, Timeout.create(Duration.ofSeconds(2)));
        System.out.println(ask);
        return ask;
    }


    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("client");
        try{
            ActorRef actorRef = system.actorOf(HttpClientSingleRequestInActorExample.props());
            actorRef.tell("http://localhost:25520/auction", ActorRef.noSender());
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
