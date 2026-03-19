package com.quiz.dto.admin;

import lombok.Data;

@Data
public class AiGenerateDTO {
    private Long questionId;
    /**
     * 生成模式：GENERATE_ANALYSIS/GENERATE_ANSWER/GENERATE_BOTH
     */
    private String mode;
}
