package com.quiz.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AiBatchGenerateDTO {
    private Long bankId;
    private List<Long> questionIds;
    private String mode;
}
