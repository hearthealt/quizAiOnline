package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("ai_config")
public class AiConfig implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * AI 提供商: OPENAI / DEEPSEEK / CUSTOM
     */
    private String provider;

    private String baseUrl;

    private String apiKey;

    private String model;

    private Integer maxTokens;

    private BigDecimal temperature;

    private Integer status;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
