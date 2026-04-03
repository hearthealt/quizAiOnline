package com.quiz.dto.admin;

import lombok.Data;

@Data
public class QuestionBankDTO {
    private Long categoryId;
    private String name;
    private String description;
    private String cover;
    private Integer passScore;
    private Integer sort;
    private Integer status;
}
