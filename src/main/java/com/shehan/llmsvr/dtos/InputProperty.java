package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputProperty {
    private String defaultValue;
    private String displayName;
    private String name;
    private Boolean required;
    private String type;
    private String value;
    private List<String> values;

    @SuppressWarnings("unchecked")
    public static InputProperty fromMap(Map<String, Object> map) {
        if (map == null) return null;

        InputProperty prop = new InputProperty();
        prop.setDefaultValue((String) map.get("defaultValue"));
        prop.setDisplayName((String) map.get("displayName"));
        prop.setName((String) map.get("name"));
        prop.setRequired((Boolean) map.get("required"));
        prop.setType((String) map.get("type"));
        prop.setValue((String) map.get("value"));
        prop.setValues((List<String>) map.get("values"));

        return prop;
    }
}
