package com.study.akka.myapp;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * @author wzj
 * @date 2021/08/31
 */
public class HttpServerWithJackJson extends AllDirectives {
    public static void main(String[] args) throws Exception{
        // boot up server using the route as defined below
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "routes");

        final Http http = Http.get(system);

        //In order to access all directives we need an instance where the routes are define.
        HttpServerWithJackJson app = new HttpServerWithJackJson();

        final CompletionStage<ServerBinding> binding = http.newServerAt("localhost", 25520)
                .bind(app.createRoute());


        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done

    }

    // (fake) async database query api
    private CompletionStage<Optional<Item>> fetchItem(long itemId){
        return CompletableFuture.completedFuture(Optional.of(new Item("foo", itemId)));
    }
    // (fake) async database query api
    private CompletionStage<Done> saveOrder(final Order order){
        System.out.println("saveOrder in");
        return CompletableFuture.completedFuture(Done.getInstance());
    }

    private Route createRoute(){
        return concat(
                get(() -> pathPrefix("item", () ->
                        path(PathMatchers.longSegment(), (Long id) -> {
                            final CompletionStage<Optional<Item>> funtureMaybeItem = fetchItem(id);
                            return onSuccess(funtureMaybeItem, maybeItem ->
                                    maybeItem.map(item -> completeOK(item, Jackson.marshaller()))
                                            .orElseGet(() -> complete(StatusCodes.NOT_FOUND, "Not Found")));
                        }))),
                get(() -> path("mypath", () -> {
                    return onSuccess(CompletableFuture.completedFuture("lalalal"), done -> complete("xxxxx"));
                })),
                post(() -> path("create-order", () -> entity(Jackson.unmarshaller(Order.class), order -> {
//                    CompletionStage<Done> futureSaved = saveOrder(order);
                    CompletionStage<Done> futureSavedv1 = CompletableFuture.completedFuture(Done.getInstance());
                    return onSuccess(futureSavedv1, done -> complete("order created"));
                })))
        );
    }


    private static class Item {
        final String name;

        final long id;

        @JsonCreator
        public Item(@JsonProperty("name") String name,
                    @JsonProperty("id") long id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public long getId() {
            return id;
        }
    }

    private static class Order {
        final List<Item> items;

        @JsonCreator
        public Order(@JsonProperty("items") List<Item> items) {
            this.items = items;
        }

        public List<Item> getItems() {
            return items;
        }
    }
}
