package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageItem {
    private String id;
    private Map<String, Object> data;
    private Map<String, Object> metadata;
    private Long timestamp;

    /**
     * Constructor with just data (most common use case)
     */
    public MessageItem(Map<String, Object> data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
        this.metadata = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Create a message item with specific data
     */
    public static MessageItem of(Map<String, Object> data) {
        return new MessageItem(data);
    }

    /**
     * Create a message item with a single key-value pair
     */
    public static MessageItem of(String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        return new MessageItem(data);
    }

    /**
     * Builder pattern for complex construction
     */
    public static MessageItemBuilder builder() {
        return new MessageItemBuilder();
    }

    public static class MessageItemBuilder {
        private String id;
        private Map<String, Object> data = new HashMap<>();
        private Map<String, Object> metadata = new HashMap<>();
        private Long timestamp;

        public MessageItemBuilder id(String id) {
            this.id = id;
            return this;
        }

        public MessageItemBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public MessageItemBuilder addData(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public MessageItemBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MessageItemBuilder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public MessageItemBuilder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public MessageItem build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            if (timestamp == null) {
                timestamp = System.currentTimeMillis();
            }
            return new MessageItem(id, data, metadata, timestamp);
        }
    }

    /**
     * Get a specific data field
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return data != null ? (T) data.get(key) : null;
    }

    /**
     * Get a specific data field with default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        if (data == null || !data.containsKey(key)) {
            return defaultValue;
        }
        return (T) data.get(key);
    }

    /**
     * Set a data field
     */
    public void setData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }

    /**
     * Get a metadata field
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return metadata != null ? (T) metadata.get(key) : null;
    }

    /**
     * Set a metadata field
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * Check if data contains a key
     */
    public boolean hasData(String key) {
        return data != null && data.containsKey(key);
    }

    /**
     * Get data as a specific type safely
     */
    public String getDataAsString(String key) {
        Object value = getData(key);
        return value != null ? value.toString() : null;
    }

    public Integer getDataAsInt(String key) {
        Object value = getData(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    public Double getDataAsDouble(String key) {
        Object value = getData(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    public Boolean getDataAsBoolean(String key) {
        Object value = getData(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
