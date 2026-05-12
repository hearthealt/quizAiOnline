package com.quiz.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDTO {
    private Long bankId;
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer sort;
    private Integer status;
    private List<OptionDTO> options;

    @Data
    public static class OptionDTO {
        private String label;
        private String content;
    }
}
