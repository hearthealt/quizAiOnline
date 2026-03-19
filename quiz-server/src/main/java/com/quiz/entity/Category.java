package com.quiz.entity;

import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("category")
public class Category extends BaseEntity {

    private String name;

    private String icon;

    private Integer sort;

    private Integer status;
}
