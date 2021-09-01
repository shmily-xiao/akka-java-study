package com.study.akka.myapp.server;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * @author wzj
 * @date 2021/08/31
 */
public class HttpServerWithStreaming extends AllDirectives {

    public static void main(String[] args) throws Exception {
        // boot up server using the routes as defined below
        ActorSystem<Void> system  = ActorSystem.create(Behaviors.empty(), "routes");
        final Http http = Http.get(system);

        HttpServerWithStreaming app = new HttpServerWithStreaming();

        final CompletionStage<ServerBinding> binding = http.newServerAt("localhost", 25520)
                .bind(app.createRoute());

        System.out.println("Server online at http://localhost:25520/\n Press RETURN to stop ...");
        System.in.read(); // let it run until user presses return

        binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> {
                    System.out.println("thenAccept run");
                    System.out.println(unbound);
                    system.terminate();  // and shutdown when done
                });
    }

    private Route createRoute() {
        final Random rnd = new Random();
        // streams are re-usable so we can define it here
        // and use it for every request
        Source<Integer, NotUsed> numbers = Source.fromIterator(() -> Stream.generate(rnd::nextInt).iterator());

        return concat(
                path("random", () -> get(() ->
                        complete(HttpEntities.create(ContentTypes.TEXT_PLAIN_UTF8,
                                // transform each number to a chunk of bytes
                                numbers.map(x -> ByteString.fromString(x + "\n"))
                                )))),
                get(() -> path("test1", () -> {
                    return onSuccess(CompletableFuture.completedFuture("lalalal"), done -> complete("xxxxx"));
                })),
                path("test", () -> get(() ->
                        complete("yyyy")))
        );
    }
}
