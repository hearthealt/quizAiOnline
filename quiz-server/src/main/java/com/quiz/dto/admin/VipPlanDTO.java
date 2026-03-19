package com.quiz.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VipPlanDTO {
    private String name;
    private Integer duration;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private Integer sort;
    private Integer status;
}
