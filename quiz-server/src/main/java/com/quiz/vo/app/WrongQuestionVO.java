package com.quiz.vo.app;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WrongQuestionVO {
    private Long id;
    private Long questionId;
    private Long bankId;
    private String bankName;
    private String content;
    private Integer type;
    private String answer;
    private String analysis;
    private List<OptionVO> options;
    private Integer wrongCount;
    private String lastWrongAnswer;
    private LocalDateTime createTime;

    @Data
    public static class OptionVO {
        private String label;
        private String content;
    }
}
