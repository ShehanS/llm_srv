package com.shehan.llmsvr.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ModelConfig  implements Serializable {

    @Column(name = "model_provider")
    private String provider;

    @Column(name = "model_name")
    private String name;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "api_key")
    private String apiKey;
}
