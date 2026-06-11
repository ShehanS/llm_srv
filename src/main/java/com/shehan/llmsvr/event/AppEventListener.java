package com.shehan.llmsvr.event;

import com.shehan.llmsvr.config.DynamicRouteHolder;
import com.shehan.llmsvr.config.HttpRouteBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppEventListener {
    @Value("${intelligent-srv.url}")
    private String intelligentSrvUrl;

    private final HttpRouteBuilder builder;
    private final DynamicRouteHolder holder;
    private final WebClient.Builder webClientBuilder;


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Netty server is UP and ready, reload routes");
        builder.buildAsync()
                .doOnNext(holder::update)
                .doOnNext(r -> log.info("Routes reloaded after workflow change"))
                .subscribe();
    }


    //@EventListener
    public void onWorkflowChanged(WorkflowChangedEvent event) {
        builder.buildAsync()
                .doOnNext(holder::update)
                .doOnNext(r -> log.info("Routes reloaded after workflow change"))
                .subscribe();
    }


    //@EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        log.info("Configuration change detected for route: {}. Notifying AI Service...");
        String aiServiceUrl = intelligentSrvUrl + "/api/v1/fetch-config/" + event.getRouteAgent();

        webClientBuilder.build()
                .get()
                .uri(aiServiceUrl)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info("AI Service successfully reloaded: {}", response),
                        error -> log.error("Failed to notify AI Service: {}", error.getMessage())
                );
    }
}

