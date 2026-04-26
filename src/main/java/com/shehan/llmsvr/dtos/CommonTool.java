package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.CommonToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonTool extends BaseClass<CommonTool, CommonToolEntity> {
    private Integer id;
    private String source;
    private String toolName;
    private String toolDisplayName;
    private String type;
    private String description;
    private Boolean readOnly;
}
