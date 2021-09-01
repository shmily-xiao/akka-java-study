package com.study.akka.myapp.server;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.util.ByteString;

import java.util.concurrent.CompletionStage;

/**
 * 使用相对原始的akka-http接口来写server
 * @author wzj
 * @date 2021/08/31
 */
public class HttpServerLowLevel {


    public static void main(String[] args) {
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "lowlevel");

        try{
            CompletionStage<ServerBinding> serverBindingFuture = Http.get(system).newServerAt("localhost", 25520).bindSync(
                    request -> {
                        if (request.getUri().path().equals("/")){
                            return HttpResponse.create().withEntity(ContentTypes.TEXT_HTML_UTF8, ByteString.fromString("<html><body>hello world</body></html>"));

                        }else if (request.getUri().path().equals("/ping")){
                            return HttpResponse.create().withEntity(ByteString.fromString("PONG!"));
                        }else if (request.getUri().path().equals("/crash")){
                            throw new RuntimeException("Boom!");
                        } else {
                            request.discardEntityBytes(system);
                            return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND).withEntity("Unknown Resource!");
                        }
                    }
            );
            System.out.println("Server is  online http://localhost:25520");
            System.in.read();

            serverBindingFuture.thenCompose(ServerBinding::unbind)
                    .thenAccept(unbound -> system.terminate());

        }catch ( Exception e){
            system.terminate();
        }
    }

}
