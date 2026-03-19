package com.quiz.vo.app;

import lombok.Data;

import java.util.List;

@Data
public class QuestionVO {
    private Long id;
    private Long bankId;
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private List<OptionVO> options;
    private Boolean isFavorite;
    private String userAnswer;
    private Integer isCorrect;

    @Data
    public static class OptionVO {
        private String label;
        private String content;
    }
}
