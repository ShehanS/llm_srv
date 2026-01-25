package com.shehan.llmsvr.entites;

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
public class AgentEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, name = "agent_name")
    private String agentName;

    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String expertise;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Embedded
    private ModelConfig model;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @ManyToMany(mappedBy = "agents")
    private Set<RoutingConfigEntity> routingConfigs = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "agent_tool_names",
            joinColumns = @JoinColumn(name = "agent_id")
    )
    @Column(name = "tools")
    private Set<String> tools = new HashSet<>();
}
