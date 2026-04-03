package com.quiz.vo.app;

import lombok.Data;

@Data
public class QuestionListVO {
    private Long id;
    private Long bankId;
    private Integer type;
    private String content;
    private Integer difficulty;
}
