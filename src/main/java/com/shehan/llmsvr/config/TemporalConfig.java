package com.shehan.llmsvr.config;

import com.shehan.llmsvr.nodes.WorkflowNode;
import com.shehan.llmsvr.service.impl.DynamicNodeRouterImpl;
import com.shehan.llmsvr.service.impl.WorkflowOrchestratorImpl;
import com.shehan.llmsvr.service.impl.TraceActivityImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class TemporalConfig {

    private static final String TASK_QUEUE = "AI_WORKFLOW_TASK_QUEUE";

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    @Configuration
    @RequiredArgsConstructor
    public static class WorkerSetup {
        private final WorkflowClient workflowClient;
        private final List<WorkflowNode> workflowNodes;
        private final TraceActivityImpl traceActivityImpl;

        @PostConstruct
        public void startTemporalWorker() {
            WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
            Worker worker = factory.newWorker(TASK_QUEUE);

            worker.registerWorkflowImplementationTypes(WorkflowOrchestratorImpl.class);
            worker.registerActivitiesImplementations(traceActivityImpl);
            worker.registerActivitiesImplementations(new DynamicNodeRouterImpl(workflowNodes));

            factory.start();
            log.info("Temporal Dynamic routing engine online on queue: {}", TASK_QUEUE);
        }
    }
}