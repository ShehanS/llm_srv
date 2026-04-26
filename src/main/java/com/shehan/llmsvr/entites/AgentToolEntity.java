package com.shehan.llmsvr.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shehan.llmsvr.enums.ToolType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "agent_tools")
@ToString(exclude = "agents")
public class AgentToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false)
    private String toolName;

    @Column(length = 50, nullable = false)
    private String toolDisplayName;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private ToolType type;

    @Column(columnDefinition = "TEXT")
    private String source;

    @Builder.Default
    @Column(nullable = false)
    private Boolean dangerous = false;

    @JsonIgnore
    @ManyToMany(mappedBy = "tools", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<AgentEntity> agents = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String copyFrom;

    private Boolean readOnly;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentToolEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
