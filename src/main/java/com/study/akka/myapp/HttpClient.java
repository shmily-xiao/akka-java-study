package com.study.akka.myapp;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.dispatch.ThreadPoolConfig;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 目前只是会发起请求，但是还不能拿到结果
 * @author wzj
 * @date 2021/08/31
 */
public class HttpClient {
    public static void main(String[] args) {
        final ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "client");

        final CompletionStage<HttpResponse> responseCompletionStage = Http.get(system)
                .singleRequest(HttpRequest.create("http://localhost:25520/auction"));

        responseCompletionStage.thenAccept(item -> {
            System.out.println(item);
        });

    }

}
