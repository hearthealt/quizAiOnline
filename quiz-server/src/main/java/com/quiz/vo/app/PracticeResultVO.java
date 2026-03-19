package com.quiz.vo.app;

import lombok.Data;

@Data
public class PracticeResultVO {
    private Long recordId;
    private Integer totalCount;
    private Integer answerCount;
    private Integer correctCount;
    private Double correctRate;
    private Integer duration;
}
