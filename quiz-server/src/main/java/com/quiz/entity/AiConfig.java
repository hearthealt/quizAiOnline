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

    private String baseUrl;

    private String apiKey;

    private String model;

    private String promptAnalysis;

    private String promptAnswer;

    private String promptBoth;

    private Integer maxTokens;

    private BigDecimal temperature;

    private Integer status;

    /**
     * iFlow 平台认证 Cookie（BXAuth）
     */
    private String bxAuth;

    /**
     * iFlow 平台用户标识名称
     */
    private String iflowName;

    /**
     * API Key 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否自动续期（1-是 0-否）
     */
    private Integer autoRenew;

    /**
     * 上次续期时间
     */
    private LocalDateTime lastRenewTime;

    /**
     * 上次续期结果
     */
    private String lastRenewResult;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
