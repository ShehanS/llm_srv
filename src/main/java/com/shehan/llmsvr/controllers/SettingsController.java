package com.shehan.llmsvr.controllers;

import com.shehan.llmsvr.dtos.ResponseCode;
import com.shehan.llmsvr.dtos.ResponseMessage;
import com.shehan.llmsvr.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@CrossOrigin("*")
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @PostMapping(value = "/tool/install", consumes = "multipart/form-data")
    public Mono<ResponseEntity<ResponseMessage>> installTool(
            @RequestPart("file") Mono<FilePart> filePart) {

        log.info("API Request: Install tool");

        return settingsService.install(filePart)
                .map(tool -> ResponseEntity.ok(
                        new ResponseMessage(
                                ResponseCode.SUCCESS.getCode(),
                                ResponseCode.SUCCESS.getMessage(),
                                tool,
                                null)))
                .onErrorResume(e -> {
                    log.error("Error installing tool: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError().body(
                            new ResponseMessage(
                                    ResponseCode.ERROR.getCode(),
                                    ResponseCode.ERROR.getMessage(),
                                    null,
                                    e.getMessage()
                            )));
                });
    }

    @GetMapping(path = "/tool/all")
    public Mono<ResponseEntity<ResponseMessage>> getTools() {
        log.info("API Request: Get all tools");
        return settingsService.getTools()
                .collectList()
                .map(tools ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Tools retrieved successfully",
                                        tools,
                                        null
                                )
                        )
                );
    }

    @GetMapping(path = "/agents/all")
    public Mono<ResponseEntity<ResponseMessage>> getAgents() {
        log.info("API Request: Get all agents");
        return settingsService.getAgents()
                .collectList()
                .map(tools ->
                        ResponseEntity.ok(
                                new ResponseMessage(
                                        ResponseCode.SUCCESS.getCode(),
                                        "Agents retrieved successfully",
                                        tools,
                                        null
                                )
                        )
                );
    }


    @GetMapping("/tool/{toolId}")
    public ResponseEntity<ResponseMessage> getTool(@PathVariable Long toolId) {
        log.info("API Request: Get plugin {}", toolId);
        try {
            Object plugin = settingsService.ggetTool(toolId);
            return ResponseEntity.ok(
                    new ResponseMessage(
                            ResponseCode.SUCCESS.getCode(),
                            "Tool retrieved successfully",
                            plugin,
                            null
                    )
            );

        } catch (Exception e) {
            log.error("Error retrieving plugin: {}", e.getMessage());

            return ResponseEntity.status(404).body(
                    new ResponseMessage(
                            ResponseCode.ERROR.getCode(),
                            "Tool not found",
                            null,
                            e.getMessage()
                    )
            );
        }
    }
}
