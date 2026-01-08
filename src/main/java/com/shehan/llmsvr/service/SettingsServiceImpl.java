package com.shehan.llmsvr.service;

import com.shehan.llmsvr.dtos.Agent;
import com.shehan.llmsvr.dtos.Tool;
import com.shehan.llmsvr.dtos.ToolStatus;
import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.ToolEntity;
import com.shehan.llmsvr.repositories.AgentRepository;
import com.shehan.llmsvr.repositories.PluginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsServiceImpl implements SettingsService {
    private final PluginRepository pluginRepository;
    private final AgentRepository agentRepository;

    @Value("${plugin.path}")
    private String pluginPath;

    @Override
    public Mono<Tool> install(Mono<FilePart> filePart) {
        log.info("Installing plugin....");
        return filePart
                .flatMap(file -> {
                    String fileName = file.filename();
                    Path filePath = Paths.get(pluginPath + fileName);
                    return file.transferTo(filePath)
                            .then(Mono.fromCallable(() -> filePath.toFile()))
                            .flatMap(savedFile -> readPlugin(savedFile))
                            .flatMap(tool -> savePluginWithAgents(tool))
                            .doOnSuccess(tool ->
                                    log.info("Tool installed successfully - name: {}, pId: {}",
                                            tool.getName(), tool.getPId()))
                            .doOnError(error ->
                                    log.error("Error installing plugin: {}", error.getMessage(), error));
                });
    }

    private Mono<Tool> savePluginWithAgents(Tool tool) {
        return Mono.fromCallable(() -> {
            ToolEntity entity = tool.toEntity(ToolEntity.class);
            Set<AgentEntity> resolvedAgents = new HashSet<>();
            for (AgentEntity a : tool.getAgents()) {
                String agentName = a.getAgentName();
                AgentEntity dbAgent = agentRepository.findByAgentName(agentName)
                        .orElseThrow(() -> new RuntimeException("Agent not found: " + agentName));
                resolvedAgents.add(dbAgent);
            }

            entity.setAgents(resolvedAgents);
            ToolEntity savedEntity = pluginRepository.save(entity);
            return Tool.fromEntity(savedEntity, Tool.class);
        }).subscribeOn(Schedulers.boundedElastic());
    }


    @Override
    public Tool delete(Tool tool) {
        return null;
    }

    @Override
    public Tool update(Tool tool) {
        return null;
    }

    @Override
    public Tool status(Tool tool) {
        return null;
    }

    @Override
    public Flux<Tool> getTools() {
        return Mono.fromCallable(() -> pluginRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(entity -> Tool.fromEntity(entity, Tool.class));
    }


    @Override
    public Tool getTool(Long pluginId) {
        ToolEntity toolEntity = pluginRepository.findById(pluginId)
                .orElseThrow(() -> new RuntimeException("Tool not found: " + pluginId));
        return Tool.fromEntity(toolEntity, Tool.class);
    }

    @Override
    public Flux<Agent> getAgents() {
        return Mono.fromCallable(() -> agentRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(entity -> Agent.fromEntity(entity, Agent.class));
    }


    private Mono<Tool> readPlugin(File file) {
        return Mono.fromCallable(() -> {
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Tool tool = new Tool();

                String namePattern = "name:\\s*[\"']([^\"']+)[\"']";
                Matcher nameMatcher = Pattern.compile(namePattern).matcher(content);
                if (nameMatcher.find()) {
                    tool.setName(nameMatcher.group(1));
                    tool.setPId(nameMatcher.group(1));
                }

                String descriptionPattern = "description:\\s*[\"']([^\"']+)[\"']";
                Matcher descriptionMatcher = Pattern.compile(descriptionPattern).matcher(content);
                if (descriptionMatcher.find()) {
                    tool.setDescription(descriptionMatcher.group(1));
                }

                String agentsPattern = "targetAgents:\\s*\\[([^\\]]+)\\]";
                Matcher agentsMatcher = Pattern.compile(agentsPattern).matcher(content);
                if (agentsMatcher.find()) {
                    String agentsString = agentsMatcher.group(1);
                    String[] agentNames = agentsString.replaceAll("[\"'\\s]", "").split(",");

                    Set<AgentEntity> agents = new HashSet<>();
                    for (String agentName : agentNames) {
                        AgentEntity a = new AgentEntity();
                        a.setAgentName(agentName);
                        agents.add(a);
                    }
                    tool.setAgents(agents);
                }

                tool.setFileName(file.getName());
                tool.setPath(file.getAbsolutePath());
                tool.setStatus(ToolStatus.ACTIVE);
                tool.setCreatedAt(Instant.now());

                log.info("Parsed tool - pId: {}, name: {}, description: {}, agents: {}",
                        tool.getPId(),
                        tool.getName(),
                        tool.getDescription(),
                        tool.getAgents()
                                .stream()
                                .map(AgentEntity::getAgentName)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("none")
                );

                return tool;

            } catch (IOException e) {
                log.error("Error reading plugin file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to read plugin file", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


}
