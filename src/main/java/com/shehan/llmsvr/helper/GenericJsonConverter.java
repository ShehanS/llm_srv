package com.shehan.llmsvr.helper;

import jakarta.persistence.AttributeConverter;
import tools.jackson.databind.ObjectMapper;

public abstract class GenericJsonConverter<T> implements AttributeConverter<T, String> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> type;

    protected GenericJsonConverter(Class<T> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        try {
            return attribute == null ? null : mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting to JSON", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : mapper.readValue(dbData, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting from JSON", e);
        }
    }
}
