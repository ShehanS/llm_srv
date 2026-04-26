package com.shehan.llmsvr.entites;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "agents")
@ToString(exclude = {"tools", "routingConfigs"})
public class AgentEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, name = "agent_name")
    private String agentName;

    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String environment;

    @Column(columnDefinition = "TEXT", name = "base_url")
    private String baseURL;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String expertise;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Embedded
    @Builder.Default
    private ModelConfig model = new ModelConfig();

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @JsonIgnoreProperties("agents")
    @ManyToMany(mappedBy = "agents")
    @Builder.Default
    private Set<RoutingAgentEntity> routingConfigs = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "agent_tool_links",
            joinColumns = @JoinColumn(name = "agent_id"),
            inverseJoinColumns = @JoinColumn(name = "tool_id")
    )
    @Builder.Default
    private Set<AgentToolEntity> tools = new HashSet<>();

    public void addTool(AgentToolEntity tool) {
        this.tools.add(tool);
        tool.getAgents().add(this);
    }

    public void removeTool(AgentToolEntity tool) {
        this.tools.remove(tool);
        tool.getAgents().remove(this);
    }
}
