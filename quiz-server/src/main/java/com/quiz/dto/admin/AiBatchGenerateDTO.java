package com.quiz.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AiBatchGenerateDTO {
    private Long bankId;
    private List<Long> questionIds;
    private String mode;
    /**
     * 本次批量生成并发数，后端会按配置的最大值做限制。
     */
    private Integer concurrency;
    /**
     * 是否强制覆盖已有答案/解析。
     */
    private Boolean overwrite;
}
