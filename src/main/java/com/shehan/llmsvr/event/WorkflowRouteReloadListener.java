package com.shehan.llmsvr.event;

import com.shehan.llmsvr.config.DynamicRouteHolder;
import com.shehan.llmsvr.config.HttpRouteBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowRouteReloadListener {

    private final HttpRouteBuilder builder;
    private final DynamicRouteHolder holder;

    @EventListener
    public void onWorkflowChanged(WorkflowChangedEvent event) {

        builder.buildAsync()
                .doOnNext(holder::update)
                .doOnNext(r -> log.info("Routes reloaded after workflow change"))
                .subscribe();
    }
}
