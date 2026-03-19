package com.quiz.entity;

import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("question_bank")
public class QuestionBank extends BaseEntity {

    private Long categoryId;

    private String name;

    private String description;

    private String cover;

    private Integer questionCount;

    private Integer practiceCount;

    private Integer examTime;

    private Integer examQuestionCount;

    private Integer passScore;

    private Integer sort;

    private Integer status;
}
