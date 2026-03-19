package com.quiz.vo.app;

import lombok.Data;

import java.util.List;

@Data
public class ExamSessionVO {

    private Long examId;

    private Integer totalCount;

    /**
     * 考试时长（分钟）
     */
    private Integer examTime;

    /**
     * 剩余时间（秒）
     */
    private Integer leftSeconds;

    private List<ExamQuestionVO> questions;

    private List<ExamAnswerVO> answers;

    @Data
    public static class ExamQuestionVO {
        private Long id;
        private Integer type;
        private String content;
        private List<ExamQuestionOptionVO> options;
    }

    @Data
    public static class ExamQuestionOptionVO {
        private String label;
        private String content;
    }

    @Data
    public static class ExamAnswerVO {
        private Long questionId;
        private String answer;
    }
}
