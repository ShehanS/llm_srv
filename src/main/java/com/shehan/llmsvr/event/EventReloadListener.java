package com.shehan.llmsvr.event;

import com.shehan.llmsvr.config.DynamicRouteHolder;
import com.shehan.llmsvr.config.HttpRouteBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReloadListener {

    private final HttpRouteBuilder builder;
    private final DynamicRouteHolder holder;
    private final WebClient.Builder webClientBuilder;

    @EventListener
    public void onWorkflowChanged(WorkflowChangedEvent event) {

        builder.buildAsync()
                .doOnNext(holder::update)
                .doOnNext(r -> log.info("Routes reloaded after workflow change"))
                .subscribe();
    }


    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        log.info("Configuration change detected for route: {}. Notifying AI Service...");
        String aiServiceUrl = "http://localhost:8500/api/v1/reload";

        webClientBuilder.build()
                .post()
                .uri(aiServiceUrl)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info("AI Service successfully reloaded: {}", response),
                        error -> log.error("Failed to notify AI Service: {}", error.getMessage())
                );
    }
}

