package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.AgentEntity;
import com.shehan.llmsvr.entites.ToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tool extends BaseClass<Tool, ToolEntity> {
    private Integer id;
    private String pId;
    private String name;
    private String description;
    private String fileName;
    private String path;
    private Set<AgentEntity> agents = new HashSet<>();
    private ToolStatus status = ToolStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}
