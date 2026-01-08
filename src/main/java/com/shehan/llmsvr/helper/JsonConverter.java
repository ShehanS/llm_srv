package com.shehan.llmsvr.helper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;

@Converter
public class JsonConverter implements AttributeConverter<Object, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, Object.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
