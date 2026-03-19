package com.quiz.dto.app;

import lombok.Data;

@Data
public class SaveExamAnswerDTO {
    private Long questionId;
    private String answer;
}
