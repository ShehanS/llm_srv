package com.shehan.llmsvr.dtos;

import lombok.Data;
import org.modelmapper.ModelMapper;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
public abstract class BaseClass<T, D> implements Serializable {
    private static final ModelMapper modelMapper = new ModelMapper();

    public D toEntity(Class<D> type) {
        D entity = modelMapper.map(this, type);
        return entity;
    }

    public static <T extends BaseClass<T, D>, D> T fromEntity(D entity, Class<T> dtoClass) {
        T dto = modelMapper.map(entity, dtoClass);
        return dto;
    }

    public static <T extends BaseClass<T, D>, D> List<T> fromEntityList(List<D> entities, Class<T> dtoClass) {
        return entities.stream()
                .map(entity -> modelMapper.map(entity, dtoClass))
                .collect(Collectors.toList());
    }
}
