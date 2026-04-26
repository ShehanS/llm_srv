package com.shehan.llmsvr.entites;

import com.shehan.llmsvr.enums.ToolType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "common_tools")
public class CommonToolEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String source;

    @Column(length = 50, nullable = false)
    private String toolName;

    @Column(length = 50, nullable = false)
    private String toolDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private ToolType type;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    private Boolean readOnly;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean dangerous = false;
}
