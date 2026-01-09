package com.shehan.llmsvr.helper;

import com.shehan.llmsvr.dtos.Definition;
import jakarta.persistence.Converter;

@Converter
public class DefinitionConverter extends GenericJsonConverter<Definition> {
    public DefinitionConverter() {
        super(Definition.class);
    }
}
