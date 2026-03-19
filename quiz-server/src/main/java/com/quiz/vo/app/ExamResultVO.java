package com.quiz.vo.app;

import lombok.Data;

import java.util.List;

@Data
public class ExamResultVO {
    private Long examId;
    private Integer score;
    private Integer totalCount;
    private Integer correctCount;
    private Double correctRate;
    private Integer duration;
    private Integer passScore;
    private Boolean isPass;
    private List<ExamAnswerDetail> details;

    @Data
    public static class ExamAnswerDetail {
        private Long questionId;
        private String content;
        private String userAnswer;
        private String correctAnswer;
        private String analysis;
        private Boolean isCorrect;
    }
}
