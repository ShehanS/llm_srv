package com.shehan.llmsvr.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class LLMService {

    @GetMapping(path = "/test")
    public Mono<String> test() {
        return Mono.just("App is running");
    }

    @PostMapping(path = "/test")
    public Mono<Object> postTest(@RequestBody Object rquest) {
        return Mono.just(Map.of("status", "pass"));
    }
}
