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
@Table(name = "dangerous_tools")
public class DangerousToolEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String toolName;

    @Column
    private Boolean dangerous;

}
