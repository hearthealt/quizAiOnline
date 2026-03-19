package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("practice_detail")
public class PracticeDetail implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long recordId;

    private Long questionId;

    private String userAnswer;

    private Integer isCorrect;

    private Integer answerTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
