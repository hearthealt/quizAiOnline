package com.quiz.dto.app;

import lombok.Data;

import java.util.List;

@Data
public class SubmitExamDTO {

    private List<Answer> answers;

    @Data
    public static class Answer {
        private Long questionId;
        private String answer;
    }
}
