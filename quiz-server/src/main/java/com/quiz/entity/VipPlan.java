package com.quiz.entity;

import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("vip_plan")
public class VipPlan extends BaseEntity {

    private String name;

    private Integer duration;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String description;

    private Integer sort;

    private Integer status;
}
