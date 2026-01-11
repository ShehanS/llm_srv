package com.shehan.llmsvr.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class HttpTriggerRouteConfig {

    private final DynamicRouteHolder holder;

    @Bean
    public RouterFunction<ServerResponse> httpTriggerRoutes() {
        // delegate to current routes
        return request -> holder.get().route(request);
    }
}
