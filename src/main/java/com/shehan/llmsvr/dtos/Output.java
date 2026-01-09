package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Output {
    private String id;
    private String label;
    private String position;
}
