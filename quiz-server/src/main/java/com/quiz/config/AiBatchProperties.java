package com.quiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "quiz.ai")
public class AiBatchProperties {

    /**
     * 批量生成默认并发数，前端未传时使用。
     */
    private Integer batchConcurrency = 5;

    /**
     * 批量生成最大并发数，避免前端传入过大值打满 AI 接口。
     */
    private Integer batchMaxConcurrency = 10;
}
