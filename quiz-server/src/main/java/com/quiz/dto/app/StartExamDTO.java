package com.quiz.dto.app;

import lombok.Data;

@Data
public class StartExamDTO {
    private Long bankId;
    /**
     * 是否重新开始考试（true=结束当前未完成考试并重新开始）
     */
    private Boolean restart;
}
