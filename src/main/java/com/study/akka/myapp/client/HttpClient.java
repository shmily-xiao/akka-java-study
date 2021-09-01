package com.study.akka.myapp.client;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.dispatch.ThreadPoolConfig;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpMessage;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static akka.util.ByteString.emptyByteString;

/**
 * 目前只是会发起请求，
 * @author wzj
 * @date 2021/08/31
 */
public class HttpClient {
    public static void main(String[] args) {
        final ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "client");

        final CompletionStage<HttpResponse> responseCompletionStage = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction"));

        responseCompletionStage.thenAccept(response -> {
            System.out.println(response);
            // 拿到response的结果
            CompletionStage<Bids> pet = Jackson.unmarshaller(Bids.class).unmarshal(response.entity(), system);
            pet.thenAccept(item -> System.out.println(item)); // Bids{bids=[]}

        });

    }

    static class Bids {
        public List<String> bids;

        public Bids() {
        }

        public Bids(List<String> bids) {
            this.bids = bids;
        }

        @Override
        public String toString() {
            return "Bids{" +
                    "bids=" + bids +
                    '}';
        }
    }

    /**
     * 只是一个伪代码
     */
    private void testOne(){
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final ExecutionContextExecutor dispatcher = system.dispatcher();

        final HttpResponse response = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction")).toCompletableFuture().join();

        /* some transformation here */
        final Function<ByteString, ByteString> transformEachLine = line -> line;

        final int maximumFrameLength = 256;

        response.entity()
                .getDataBytes()
                .via(Framing.delimiter(ByteString.fromString("\n"), maximumFrameLength, FramingTruncation.ALLOW))
                .map(transformEachLine::apply)
                .runWith(FileIO.toPath(new File("/tmp/excample.out").toPath()),system);

    }

    final class ExamplePerson{
        final String name;

        public ExamplePerson(String name) {
            this.name = name;
        }
    }

    public ExamplePerson parse(ByteString line){
        return new ExamplePerson(line.utf8String());
    }

    /**
     * 可以使用流
     */
    private void testTwo(){
        final  akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final  ExecutionContextExecutor dispatcher = system.dispatcher();

        final HttpResponse response = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction")).toCompletableFuture().join();


        // toStrict to enforce all data be loaded into memory from the connection
        // toStrict 会强制将会话的所有数据倒入到内存中
        final  CompletionStage<HttpEntity.Strict> strictEntity = response.entity()
                .toStrict(FiniteDuration.create(3, TimeUnit.SECONDS).toMillis(), system);

        // You can now use getData to get the data directly
        // 可以直接通过getData的方法使用
        final CompletionStage<ExamplePerson> person1 =
                strictEntity.thenApply(strict -> parse(strict.getData()));

        // Though it is also still possible to use the streaming Api to consumer dataBytes,even though now they're in memory:
        // 即使是放在内存里面，我们也依然可以使用 流式 的API 来消费数据
        final CompletionStage<ExamplePerson> person2 =
                strictEntity
                        .thenCompose(strict -> strict
                                .getDataBytes()
                                .runFold(emptyByteString(), (acc, b) -> acc.concat(b), system)
                                .thenApply(this::parse)
                        );

    }


    public CompletionStage<ExamplePerson> runRequest(HttpRequest request, akka.actor.ActorSystem system) {
        // run a single request, consuming it completely in a single stream
        // 一个单独的请求，就在一个单独的流里面进行消费

        return Http.get(system)
                .singleRequest(request)
                .thenCompose(response ->
                        response.entity().getDataBytes()
                                .runReduce((a,b) -> a.concat(b), system)
                                .thenApply(this::parse)
                );
    }

    private void testThree(){
        final  akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final  ExecutionContextExecutor dispatcher = system.dispatcher();

        final HttpResponse response = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction")).toCompletableFuture().join();


        final List<HttpRequest> requests = new ArrayList<>();

        final Flow<ExamplePerson, Integer, NotUsed> exampleProcessingFlow = Flow.fromFunction(person -> person.toString().length());

        // via 取道，通过；经由
        final  CompletionStage<Done> stream = Source.from(requests)
                .mapAsync(1, item -> this.runRequest(item, system))
                .via(exampleProcessingFlow)
                .runWith(Sink.ignore(), system);

    }

    /**
     * 丢弃响应
     */
    private void testFour(){
        final  akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final  ExecutionContextExecutor dispatcher = system.dispatcher();

        final HttpResponse response = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction")).toCompletableFuture().join();

        final HttpMessage.DiscardedEntity discardedEntity = response.discardEntityBytes(system);

        discardedEntity.completionStage().whenComplete((done, ex) ->{
            System.out.println("Entity discarded completely!");
        });
    }


    /**
     * 丢弃响应
     */
    private void testFourV2(){
        final  akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final  ExecutionContextExecutor dispatcher = system.dispatcher();

        final HttpResponse response = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction")).toCompletableFuture().join();


        final CompletionStage<Done> discardingComplete = response.entity()
                .getDataBytes().runWith(Sink.ignore(), system);

        discardingComplete.whenComplete((done, ex) ->{
            System.out.println("Entity discarded completely!");
        });
    }






}
