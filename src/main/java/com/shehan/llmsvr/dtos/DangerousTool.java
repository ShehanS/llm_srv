package com.shehan.llmsvr.dtos;

import com.shehan.llmsvr.entites.DangerousToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DangerousTool extends BaseClass<DangerousTool, DangerousToolEntity> {
    private Integer id;
    private String toolName;
    private Boolean dangerous;

}
