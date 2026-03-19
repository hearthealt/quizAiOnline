package com.quiz.dto.admin;

import lombok.Data;

@Data
public class AdminQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
}
