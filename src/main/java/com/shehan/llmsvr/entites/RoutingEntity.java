package com.shehan.llmsvr.entites;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "routing_configs")
public class RoutingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "provider", column = @Column(name = "classifier_provider")),
            @AttributeOverride(name = "name", column = @Column(name = "classifier_model_name"))
    })
    private ModelConfig classifierModel;

    private String fallbackAgent;

    @Column(columnDefinition = "TEXT")
    private String routingPrompt;
}
