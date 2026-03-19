package com.quiz.dto.admin;

import lombok.Data;

@Data
public class CategoryDTO {
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
}
