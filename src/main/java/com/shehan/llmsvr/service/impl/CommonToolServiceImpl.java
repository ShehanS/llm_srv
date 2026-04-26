package com.shehan.llmsvr.service.impl;

import com.shehan.llmsvr.entites.CommonToolEntity;
import com.shehan.llmsvr.repositories.CommonToolRepository;
import com.shehan.llmsvr.service.CommonToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Service
public class CommonToolServiceImpl implements CommonToolService {
    private final CommonToolRepository commonToolRepository;

    @Override
    public Flux<CommonToolEntity> loadAllCommonTools() {
        return Flux.fromIterable(commonToolRepository.findAll());
    }

    @Override
    public Mono<CommonToolEntity> loadCommonTool(String toolName) {
        return Mono.fromCallable(() -> commonToolRepository.getToolByName(toolName));
    }
}
