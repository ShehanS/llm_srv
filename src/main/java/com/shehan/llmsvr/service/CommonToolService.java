package com.shehan.llmsvr.service;

import com.shehan.llmsvr.entites.CommonToolEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommonToolService {
    Flux<CommonToolEntity> loadAllCommonTools();

    Mono<CommonToolEntity> loadCommonTool(String toolName);
}
