package com.shehan.llmsvr.entites;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

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

    @Column(columnDefinition = "TEXT")
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

    @Column(columnDefinition = "JSON")
    private String tools;
}
