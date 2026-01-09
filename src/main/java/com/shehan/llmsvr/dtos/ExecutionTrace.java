package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace {
    private String runId;
    private String nodeId;
    private String nodeType;
    private Object input;
    private Object output;
    private Map<String, Object> config;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String error;

    public static ExecutionTraceBuilder builder() {
        return new ExecutionTraceBuilder();
    }

    public static class ExecutionTraceBuilder {
        private String runId;
        private String nodeId;
        private String nodeType;
        private Object input;
        private Object output;
        private Map<String, Object> config;
        private String status;
        private Instant startedAt;
        private Instant completedAt;
        private String error;

        public ExecutionTraceBuilder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public ExecutionTraceBuilder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public ExecutionTraceBuilder nodeType(String nodeType) {
            this.nodeType = nodeType;
            return this;
        }

        public ExecutionTraceBuilder input(Object input) {
            this.input = input;
            return this;
        }

        public ExecutionTraceBuilder output(Object output) {
            this.output = output;
            return this;
        }

        public ExecutionTraceBuilder config(Map<String, Object> config) {
            this.config = config;
            return this;
        }

        public ExecutionTraceBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ExecutionTraceBuilder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ExecutionTraceBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ExecutionTraceBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ExecutionTrace build() {
            return new ExecutionTrace(runId, nodeId, nodeType, input, output,
                    config, status, startedAt, completedAt, error);
        }
    }
}
