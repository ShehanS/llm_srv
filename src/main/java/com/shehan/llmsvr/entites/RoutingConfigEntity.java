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
    @Column(columnDefinition = "TEXT")
    private ModelConfig classifierModel;

    @Column(columnDefinition = "TEXT")
    private String fallbackAgent;

    @Column(columnDefinition = "TEXT")
    private String routingPrompt;
}
