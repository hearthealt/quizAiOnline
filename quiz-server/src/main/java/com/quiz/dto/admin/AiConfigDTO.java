package com.quiz.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiConfigDTO {
    private String baseUrl;
    private String apiKey;
    private String model;
    private String promptAnalysis;
    private String promptAnswer;
    private String promptBoth;
    private Integer maxTokens;
    private BigDecimal temperature;
    private String bxAuth;
    private String iflowName;
    private Integer autoRenew;
}
