package com.bcaas.core.workflow.domain.model;

import java.util.UUID;

public record WorkflowId(UUID value) {
    public WorkflowId {
        if (value == null) throw new IllegalArgumentException("WorkflowId ne peut pas être null");
    }
    public static WorkflowId generate() { return new WorkflowId(UUID.randomUUID()); }
    public static WorkflowId of(String value) { return new WorkflowId(UUID.fromString(value)); }
    @Override public String toString() { return value.toString(); }
}
