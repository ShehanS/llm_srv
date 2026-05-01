package com.shehan.llmsvr.event;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class ConfigChangedEvent {
    private final String routeAgent;
}
