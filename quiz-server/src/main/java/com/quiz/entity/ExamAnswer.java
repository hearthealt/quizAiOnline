package com.quiz.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;

@Data
@Table("exam_answer")
public class ExamAnswer implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long examId;

    private Long questionId;

    private String userAnswer;

    private Integer isCorrect;
}
