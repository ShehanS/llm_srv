package com.shehan.llmsvr.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void workflowSaved() {
        publisher.publishEvent(new WorkflowChangedEvent());
    }
}
