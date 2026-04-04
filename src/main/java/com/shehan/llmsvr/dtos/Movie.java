package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Movie {
    private String title;
    private int year;
    private String director;
    private double rating;
}
