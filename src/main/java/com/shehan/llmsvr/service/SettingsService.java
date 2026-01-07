package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.Tool;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SettingsService {
    public Mono<Tool> install(Mono<FilePart> file);
    public Tool delete(Tool tool);
    public Tool update(Tool tool);
    public Tool status(Tool tool);
    public Flux<Tool> getTools();
    public Tool ggetTool(Long toolId);

    public Flux<Agent> getAgents();

}
