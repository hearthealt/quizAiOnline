package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("question")
public class Question extends BaseEntity {

    private Long bankId;

    private Integer type;

    private String content;

    private String answer;

    private String analysis;

    private Integer difficulty;

    private Integer sort;

    private Integer status;

    @Column(ignore = true)
    private List<QuestionOption> options;
}
