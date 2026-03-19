package com.quiz.dto.app;

import lombok.Data;

@Data
public class SubmitAnswerDTO {
    private Long questionId;
    private String answer;
    /**
     * 答题用时（秒）
     */
    private Integer answerTime;
}
