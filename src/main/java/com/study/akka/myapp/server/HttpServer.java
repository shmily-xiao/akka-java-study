package com.study.akka.myapp.server;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Connection;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import scala.concurrent.ExecutionContextExecutor;

import java.io.File;
import java.util.concurrent.CompletionStage;

/**
 * 动态的的地址： https://doc.akka.io/docs/akka-http/10.2.6/routing-dsl/play-comparison.html
 *
 */
public class HttpServer extends AllDirectives {

    public static void main(String[] args) throws Exception {
        // boot up server using the route as defined below
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "routes");

        final Http http = Http.get(system);

        //In order to access all directives we need an instance where the routes are define.
        HttpServer app = new HttpServer();

        final CompletionStage<ServerBinding> binding =
                http.newServerAt("localhost", 8080)
                        .bind(app.createRoute());

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private Route createRoute() {
        return concat(
                path("hello", () ->
                        get(() ->
                                complete("<h1>Say hello to akka-http</h1>"))));
    }

    class Bid {
        final String userId;
        final int bid;

        public Bid(String userId, int bid) {
            this.userId = userId;
            this.bid = bid;
        }
    }

    /**
     * 正常消费
     */
    private void testOne() throws Exception{
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final ExecutionContextExecutor dispatcher = system.dispatcher();
        final Http http = Http.get(system);
        HttpServer app = new HttpServer();


        // actor Materializer : actor 物化？ 序列化？
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        // 解码器
        final Unmarshaller<HttpEntity, Bid> asBid = Jackson.unmarshaller(Bid.class);

        final Route s = path("bid", () ->
                put (() -> entity(asBid, bid -> {
                    // incoming entity is fully consumed and converted into a Bid
                    return complete("The bid was: "+ bid);
                })));



        final CompletionStage<ServerBinding> binding =
                http.newServerAt("localhost", 8080)
                        .bind(s);
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    /**
     * 文件流
     * @throws Exception
     */
    private void testTwo() throws Exception{
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final ExecutionContextExecutor dispatcher = system.dispatcher();
        final Http http = Http.get(system);
        HttpServer app = new HttpServer();
        // actor Materializer : actor 物化？ 序列化？
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Route s =
                put(() ->
                    path("lines", () ->
                        withoutSizeLimit(() ->
                            extractDataBytes(bytes -> {
                                final CompletionStage<IOResult> res = bytes.runWith(FileIO.toPath(new File("/tmp/example.out").toPath()), materializer);

                                return onComplete(() -> res, ioResult ->
                                        // we only want to respond once the incoming data has been handled:
                                        complete("Finished writing data :" + ioResult));
                            })
                        )
                    ));


        final CompletionStage<ServerBinding> binding =
                http.newServerAt("localhost", 8080)
                        .bind(s);
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done

    }

    /**
     * 终止流
     * @throws Exception
     */
    private void testThree() throws Exception{
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create();
        final ExecutionContextExecutor dispatcher = system.dispatcher();
        final Http http = Http.get(system);
        HttpServer app = new HttpServer();

        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Route s = put(() ->
                path("lines", () -> withoutSizeLimit(() ->
                        extractDataBytes(bytes -> {
                            // 关闭连接的第一种方式（立即的）
                            // closing connections, method 1 (eager):
                            // we deem this request as illegal, and close the connection right way:
                            // "brutally" closes the connection
                            // 野蛮的关闭连接
                            bytes.runWith(Sink.cancelled(), materializer);

                            // 关闭连接的第二种方式（优美）
                            // closing connections, method 2 (graceful):
                            // consider draining connection and replying with 'Connection: Close' header
                            // if you want the client to close after this request/reply cycle instead:
                            // FORBIDDEN 被禁止的
                            return respondWithHeader(Connection.create("close"), () -> complete(StatusCodes.FORBIDDEN, "Not allowed!"));
                        })))
        );


        final CompletionStage<ServerBinding> binding =
                http.newServerAt("localhost", 8080)
                        .bind(s);
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done

    }
}
