package com.shehan.llmsvr.helper;

import com.shehan.llmsvr.dtos.WorkflowDefinition;
import jakarta.persistence.Converter;

@Converter
public class DefinitionConverter extends GenericJsonConverter<WorkflowDefinition> {
    public DefinitionConverter() {
        super(WorkflowDefinition.class);
    }
}
