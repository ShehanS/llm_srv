package com.shehan.llmsvr.helper;


import com.shehan.llmsvr.dtos.InputProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {
    private String icon;
    private List<InputProperty> inputProps;

}
