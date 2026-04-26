package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.AgentToolEntity;
import com.shehan.llmsvr.enums.ToolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTool extends BaseClass<AgentTool, AgentToolEntity> {
    private Integer id;
    private String toolName;
    private String toolDisplayName;
    private String description;
    private ToolType type;
    private String source;
    private Boolean dangerous;
    private String copyFrom;
    private Boolean readOnly;

}
