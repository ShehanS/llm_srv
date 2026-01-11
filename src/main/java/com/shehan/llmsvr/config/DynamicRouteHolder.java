package com.shehan.llmsvr.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RequestPredicates;

@Component
public class DynamicRouteHolder {
    private volatile RouterFunction<ServerResponse> routes =
            RouterFunctions.route(
                    RequestPredicates.GET("/__route_placeholder"),
                    request -> ServerResponse.notFound().build()
            );

    public RouterFunction<ServerResponse> get() {
        return routes;
    }

    public void update(RouterFunction<ServerResponse> newRoutes) {
        this.routes = newRoutes;
    }
}
