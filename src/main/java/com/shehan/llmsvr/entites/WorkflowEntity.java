package com.shehan.llmsvr.entites;

import com.shehan.llmsvr.dtos.WorkflowDefinition;
import com.shehan.llmsvr.helper.DefinitionConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Table(name = "workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class WorkflowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;
    @Column(name = "flow_id")
    private String flowId;

    @Column(name = "flow_name")
    private String flowName;

    @Column(name = "description")
    private String description;

    @Convert(converter = DefinitionConverter.class)
    @Column(columnDefinition = "TEXT")
    private WorkflowDefinition definition;

    @Column(name = "flow_state")
    private boolean state;

    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

}

