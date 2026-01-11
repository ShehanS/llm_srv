package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.config.DynamicRouteHolder;
import com.shehan.llmsvr.config.HttpRouteBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteReloadController {

    private final DynamicRouteHolder holder;
    private final HttpRouteBuilder builder;

    @PostMapping("/reload")
    public Mono<Map<String, Object>> reloadRoutes() {

        return builder.buildAsync()
                .doOnNext(holder::update)
                .doOnNext(r -> log.info("Routes reloaded successfully"))
                .map(r -> Map.of(
                        "status", "OK",
                        "message", "Routes reloaded"
                ));
    }
}
