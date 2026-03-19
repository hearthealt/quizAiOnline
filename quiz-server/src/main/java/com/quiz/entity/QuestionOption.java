package com.quiz.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;

@Data
@Table("question_option")
public class QuestionOption implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long questionId;

    private String label;

    private String content;

    private Integer sort;
}
