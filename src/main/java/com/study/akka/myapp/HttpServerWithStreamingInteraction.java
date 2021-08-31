package com.study.akka.myapp;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;

import javax.swing.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static akka.actor.typed.javadsl.AskPattern.ask;

/**
 * 有交互的 流
 * @author wzj
 * @date 2021/08/31
 */
public class HttpServerWithStreamingInteraction extends AllDirectives {

    private final ActorSystem<Auction.Message> system;
    private final ActorRef<Auction.Message> auction;

    public HttpServerWithStreamingInteraction(ActorSystem<Auction.Message> system) {
        this.system = system;
        this.auction = system;
    }

    static class Auction extends AbstractBehavior<Auction.Message> {

        interface Message{}

        public Auction(ActorContext<Message> context) {
            super(context);
        }

        private List<Bid> bids = new ArrayList<>();

        public static Behavior<Message> create(){
            return Behaviors.setup(Auction::new);
        }

        static class Bid implements Message{
            public final String userId;
            public final int offer;

            public Bid(String userId, int offer) {
                this.userId = userId;
                this.offer = offer;
            }
        }

        static class GetBids implements Message{
            public final ActorRef<Bids> replyTo;

            public GetBids(ActorRef<Bids> replyTo) {
                System.out.println(replyTo);
                this.replyTo = replyTo;
            }
        }

        static class Bids {
            public final List<Bid> bids;

            public Bids(List<Bid> bids) {
                this.bids = bids;
            }
        }

        @Override
        public Receive<Message> createReceive() {
            return newReceiveBuilder()
                    .onMessage(Bid.class, this::onBid)
                    .onMessage(GetBids.class, this::onGetBids)
                    .build();
        }

        private Behavior<Message> onBid(Bid bid){
            bids.add(bid);
            System.out.println(getContext());
            getContext().getLog().info("Bid complete: {}, {}", bid.userId, bid.offer);
            return this;
        }

        private Behavior<Message> onGetBids(GetBids getBids){
            getBids.replyTo.tell(new Bids(bids));
            return this;
        }
    }

    private Route createRoutes(){
        return concat(
                path("auction", () -> concat(
                        put(() ->
                                parameter(StringUnmarshallers.INTEGER, "bid", bid ->
                                        parameter("user", user -> {
                                            // place a bid, fire-and-forget
                                            auction.tell(new Auction.Bid(user, bid));
                                            return complete(StatusCodes.ACCEPTED, "bid placed");
                                        })
                                )),
                        get(() -> {
                            // query the actor for the current auction state
                            CompletionStage<Auction.Bids> bids = ask(auction, Auction.GetBids::new, Duration.ofSeconds(5), system.scheduler());
                            return completeOKWithFuture(bids, Jackson.marshaller());
                        }))
                        )
        );
    }

    public static void main(String[] args) throws Exception{
        // boot up server using the routes as defined below
        ActorSystem<Auction.Message> system = ActorSystem.create(Auction.create(), "routes");

        final Http http = Http.get(system);

        // In order to access all directive we need an instance where the routes are define;
        HttpServerWithStreamingInteraction app = new HttpServerWithStreamingInteraction(system);

        final CompletionStage<ServerBinding> binding = http.newServerAt("localhost", 25520)
                .bind(app.createRoutes());

        System.out.println("Server online at http://localhost:25520/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding.thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());

    }
}
