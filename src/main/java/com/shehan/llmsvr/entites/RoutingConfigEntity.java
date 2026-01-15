package com.shehan.llmsvr.entites;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "routing_configs")
public class RoutingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String routeName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "provider", column = @Column(name = "classifier_provider")),
            @AttributeOverride(name = "name", column = @Column(name = "classifier_model_name"))
    })
    private ModelConfig classifierModel;

    @Column(columnDefinition = "TEXT")
    private String fallbackAgent;

    @Column(columnDefinition = "TEXT")
    private String routingPrompt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "routing_config_agents",
            joinColumns = @JoinColumn(name = "routing_config_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id")
    )
    private Set<AgentEntity> agents = new HashSet<>();
}
