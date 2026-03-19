package com.quiz.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDetailVO {
    private Long id;
    private Long bankId;
    private String bankName;
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer sort;
    private List<OptionVO> options;

    @Data
    public static class OptionVO {
        private String label;
        private String content;
    }
}
