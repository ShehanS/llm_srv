package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.ExecutionTrace;
import com.shehan.llmsvr.dtos.MessageBatch;
import com.shehan.llmsvr.dtos.WorkflowDefinition;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

public final class WorkflowOrchestrator {

    private WorkflowOrchestrator() {}

    @ActivityInterface
    public interface TraceActivity {
        @ActivityMethod(name = "emit.trace")
        void emitTrace(ExecutionTrace trace);
    }

    @WorkflowInterface
    public interface StandardEngine {
        @WorkflowMethod
        String runTemporalWorkflow(MessageBatch startMessages, WorkflowDefinition wf, String runId);
        @SignalMethod(name = "resumeStandardSignal")
        void resumeSignal(MessageBatch humanInput, String outputHandle);
    }

    @WorkflowInterface
    public interface NodeEngine {
        @WorkflowMethod
        String runTemporalFromNode(MessageBatch batch, WorkflowDefinition wf, String startNodeId, String flowId);
        @SignalMethod(name = "resumeNodeSignal")
        void resumeSignal(MessageBatch humanInput, String outputHandle);
    }

    @WorkflowInterface
    public interface ParallelEngine {
        @WorkflowMethod
        String runTemporalMultipleNodes(MessageBatch batch, WorkflowDefinition wf, List<String> nodeIds, String flowId);
        @SignalMethod(name = "resumeParallelSignal")
        void resumeSignal(MessageBatch humanInput, String outputHandle);
    }
}