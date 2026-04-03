package com.quiz.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiConfigDTO {
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer maxTokens;
    private BigDecimal temperature;
}
