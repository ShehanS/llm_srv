package com.shehan.llmsvr.entites;

import com.shehan.llmsvr.dtos.ToolStatus;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tools")
public class ToolEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String type; // "script" or "api"

    @Column(columnDefinition = "TEXT")
    private String code; // The JavaScript string for VM2

    private String url; // Only used if type is "api"

    @Column(name = "tool_schema", columnDefinition = "JSON")
    private String toolSchema; // The JSON Schema for the LLM

    @Enumerated(EnumType.STRING)
    private ToolStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = ToolStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
