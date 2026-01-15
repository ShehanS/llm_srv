package com.shehan.llmsvr.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ModelConfig implements Serializable {

    @Column(name = "model_provider")
    private String provider; // e.g., "openai" or "anthropic"

    @Column(name = "model_name")
    private String name; // e.g., "gpt-4o" or "claude-3-5-sonnet-20240620"

    @Column(name = "temperature")
    private Double temperature; // Default 0.0

    @Column(name = "api_key", columnDefinition = "TEXT")
    private String apiKey; // Optional: Override the .env key if provided
}
