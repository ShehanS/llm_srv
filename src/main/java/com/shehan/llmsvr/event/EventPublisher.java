package com.shehan.llmsvr.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher publisher;

    public void workflowSaved() {
        publisher.publishEvent(new WorkflowChangedEvent());
    }

    public void configSaved() {
        publisher.publishEvent(new ConfigChangedEvent());
    }
}
